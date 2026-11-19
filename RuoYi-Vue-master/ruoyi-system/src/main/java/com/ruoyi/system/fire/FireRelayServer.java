package com.ruoyi.system.fire;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** TLS + challenge/HMAC admission. Unauthenticated metadata never reaches device ingestion. */
@Component
public class FireRelayServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(FireRelayServer.class);
    private static final byte[] MAGIC = {'F', 'C', 'R', '1'};
    private final FireRelayProperties config;
    private final FireDeviceProperties devices;
    private final FireIngestService ingest;
    private final FireConnections connections;
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final SecureRandom random = new SecureRandom();
    private volatile boolean running;
    private SSLServerSocket listener;
    private ExecutorService workers;
    private Semaphore sessions;
    private byte[] secret;
    private List<long[]> ranges;

    public FireRelayServer(FireRelayProperties config, FireDeviceProperties devices,
                           FireIngestService ingest, FireConnections connections) {
        this.config = config; this.devices = devices; this.ingest = ingest; this.connections = connections;
    }

    @Override public synchronized void start() {
        if (running || !config.isEnabled()) return;
        try {
            if (config.getPort() < 1 || config.getPort() > 65535 || config.getMaxConnections() < 1
                    || config.getMaxConnections() > 256 || config.getHandshakeSeconds() < 1
                    || config.getHandshakeSeconds() > 30 || !config.getNodeId().matches("[A-Za-z0-9_.-]{1,64}")
                    || devices.getIdleSeconds() < 1 || config.getAllowedSourceCidrs() == null
                    || config.getAllowedSourceCidrs().isEmpty())
                throw new IllegalArgumentException("fire.relay 参数无效");
            secret = Files.readString(Path.of(config.getSecretFile()), StandardCharsets.UTF_8).strip()
                    .getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32 || secret.length > 1024) throw new IllegalArgumentException("转发密钥至少32字节，最多1024字节");
            ranges = config.getAllowedSourceCidrs().stream().map(FireRelayServer::cidr).toList();
            char[] password = Files.readString(Path.of(config.getKeyStorePasswordFile()), StandardCharsets.UTF_8).strip().toCharArray();
            try {
                var store = KeyStore.getInstance("PKCS12");
                try (var input = Files.newInputStream(Path.of(config.getKeyStore()))) { store.load(input, password); }
                var manager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                manager.init(store, password);
                var tls = SSLContext.getInstance("TLS");
                tls.init(manager.getKeyManagers(), null, random);
                listener = (SSLServerSocket) tls.getServerSocketFactory().createServerSocket();
            } finally { Arrays.fill(password, '\0'); }
            listener.setReuseAddress(false);
            listener.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            listener.bind(new InetSocketAddress(config.getBindAddress(), config.getPort()), 16);
            sessions = new Semaphore(config.getMaxConnections());
            int workerCount = config.getMaxConnections() + 4; // Reserve room for health/authentication checks.
            workers = new ThreadPoolExecutor(workerCount, workerCount, 0, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), r -> { var t = new Thread(r, "fire-relay-session"); t.setDaemon(true); return t; });
            running = true;
            var acceptor = new Thread(this::accept, "fire-relay-accept");
            acceptor.setDaemon(true); acceptor.start();
            log.info("消防转发TLS接入已启动 {}:{}，节点={}", config.getBindAddress(), config.getPort(), config.getNodeId());
        } catch (Exception e) {
            stop();
            throw new IllegalStateException("消防转发接收启动失败 [" + config.getBindAddress() + ":" + config.getPort()
                    + "]：请核对证书、密钥文件、允许设备网段和端口", e);
        }
    }

    private void accept() {
        while (running) try {
            var socket = (SSLSocket) listener.accept();
            sockets.add(socket);
            try { workers.execute(() -> session(socket)); }
            catch (RejectedExecutionException e) { sockets.remove(socket); socket.close(); }
        } catch (IOException e) { if (running) log.error("消防转发接收连接失败", e); }
    }

    private void session(SSLSocket socket) {
        boolean acquired = false;
        try (socket) {
            socket.setSoTimeout(config.getHandshakeSeconds() * 1000);
            socket.startHandshake();
            var input = new DataInputStream(socket.getInputStream());
            var output = new DataOutputStream(socket.getOutputStream());
            byte[] nonce = new byte[32]; random.nextBytes(nonce);
            output.write(MAGIC); output.write(nonce); output.flush();
            var headerBytes = new ByteArrayOutputStream();
            var header = new DataOutputStream(headerBytes);
            int kind = input.readUnsignedByte(); header.writeByte(kind);
            String node = readText(input, header, 64);
            String ip = readText(input, header, 15);
            int port = input.readUnsignedShort(); header.writeShort(port);
            String id = readText(input, header, 36);
            byte[] supplied = input.readNBytes(32);
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update(MAGIC); mac.update(nonce); mac.update(headerBytes.toByteArray());
            if (!MessageDigest.isEqual(supplied, mac.doFinal()) || !config.getNodeId().equals(node)
                    || !UUID.fromString(id).toString().equals(id)) throw new IOException("身份认证失败");
            if (kind == 0 && ip.isEmpty() && port == 0) { output.writeByte(1); output.flush(); return; }
            if (kind != 1 || port == 0 || ranges.stream().noneMatch(range -> (ipv4(ip) & range[1]) == range[0]))
                throw new IOException("设备来源不在该节点允许的网段内");
            acquired = sessions.tryAcquire();
            if (!acquired) throw new IOException("转发连接数已达到上限");
            output.writeByte(1); output.flush();
            log.info("转发设备接入 node={} device={}:{} relayPeer={} relaySession={}", node, ip, port,
                    socket.getInetAddress().getHostAddress(), id);
            FireDeviceSession.run(socket, ip, () -> running, devices, ingest, connections);
        } catch (Exception e) {
            if (running) log.debug("转发连接已关闭 peer={}，原因={}", socket.getRemoteSocketAddress(), e.toString());
        } finally {
            if (acquired) sessions.release();
            sockets.remove(socket);
        }
    }

    private static String readText(DataInputStream input, DataOutputStream copy, int max) throws IOException {
        int length = input.readUnsignedShort();
        if (length > max) throw new IOException("转发头字段超长");
        byte[] value = input.readNBytes(length);
        if (value.length != length) throw new EOFException();
        copy.writeShort(length); copy.write(value);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(value)).toString();
    }

    private static long ipv4(String address) {
        String[] parts = address.split("\\.", -1);
        if (parts.length != 4) throw new IllegalArgumentException("IPv4地址无效");
        long value = 0;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}")) throw new IllegalArgumentException("IPv4地址无效");
            int number = Integer.parseInt(part);
            if (number > 255) throw new IllegalArgumentException("IPv4地址无效");
            value = (value << 8) | number;
        }
        return value;
    }

    private static long[] cidr(String text) {
        String[] parts = text.strip().split("/", -1);
        if (parts.length != 2) throw new IllegalArgumentException("设备网段格式必须为IPv4/CIDR");
        int bits = Integer.parseInt(parts[1]);
        if (bits < 0 || bits > 32) throw new IllegalArgumentException("设备网段掩码无效");
        long mask = bits == 0 ? 0 : (0xffffffffL << (32 - bits)) & 0xffffffffL;
        return new long[]{ipv4(parts[0]) & mask, mask};
    }

    @Override public synchronized void stop() {
        running = false;
        if (listener != null) try { listener.close(); } catch (IOException ignored) { }
        for (Socket socket : sockets) try { socket.close(); } catch (IOException ignored) { }
        if (workers != null) {
            workers.shutdownNow();
            try { workers.awaitTermination(5, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }
    @Override public boolean isRunning() { return running; }
}
