package com.ruoyi.system.fire;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared business session for direct devices and authenticated relay connections. */
final class FireDeviceSession {
    private static final Logger log = LoggerFactory.getLogger(FireDeviceSession.class);

    static void run(Socket socket, String ip, BooleanSupplier running,
                    FireDeviceProperties config, FireIngestService ingest, FireConnections connections) {
        String session = UUID.randomUUID().toString();
        var sources = new HashSet<String>();
        var stream = new Gb26875.Stream();
        long last = System.currentTimeMillis();
        connections.register(session, () -> { try { socket.close(); } catch (IOException ignored) { } });
        try (socket) {
            socket.setSoTimeout(1000);
            byte[] buffer = new byte[8192];
            var input = socket.getInputStream();
            while (running.getAsBoolean()) {
                int n;
                try {
                    n = input.read(buffer);
                } catch (SocketTimeoutException e) {
                    if (System.currentTimeMillis() - last > config.getIdleSeconds() * 1000L) break;
                    continue;
                }
                if (n < 0) break;
                last = System.currentTimeMillis();
                byte[] chunk = Arrays.copyOf(buffer, n);
                try {
                    ingest.rawChunk(session, ip, chunk);
                } catch (Exception e) {
                    log.error("TCP原始数据保存失败，不返回确认", e);
                    return;
                }
                for (Gb26875.Packet packet : stream.feed(chunk)) {
                    long generation = connections.generation(ip);
                    FireIngestService.Receipt receipt;
                    try {
                        receipt = ingest.receive(packet, session, ip);
                    } catch (Exception e) {
                        log.error("消防报文持久化失败，不发送成功确认；session={}", session, e);
                        continue;
                    }
                    if (receipt.source() != null && packet.frame() != null) {
                        if (!connections.seenCurrent(ip, session, generation)) return;
                        sources.add(ip);
                    }
                    if (receipt.reply() != null) {
                        try {
                            socket.getOutputStream().write(receipt.reply());
                            socket.getOutputStream().flush();
                        } catch (IOException e) {
                            ingest.transmission(receipt.sendId(), "FAILED", "TCP发送失败，交付情况未知");
                            throw e;
                        }
                        // receive() returns through the transaction proxy only after commit.
                        ingest.transmission(receipt.sendId(), "SENT", null);
                    }
                }
            }
        } catch (Exception e) {
            if (running.getAsBoolean()) log.warn("消防TCP会话结束 session={}，原因={}", session, e.toString());
        } finally {
            connections.unregister(session);
            for (String source : sources) connections.closed(source, session);
            if (stream.pending().length > 0)
                log.warn("消防TCP会话结束残留不完整帧 session={} hex={}", session, Gb26875.hex(stream.pending()));
        }
    }

    private FireDeviceSession() { }
}
