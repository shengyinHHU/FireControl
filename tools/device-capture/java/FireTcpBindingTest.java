import com.ruoyi.system.fire.FireConnections;
import com.ruoyi.system.fire.FireDeviceProperties;
import com.ruoyi.system.fire.FireTcpServer;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;

/** Real sockets and actual application YAML; no database, device traffic or external services. */
public class FireTcpBindingTest {
    private static int checks;

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    private static FireDeviceProperties configuration(String yaml, Map<String, Object> variables) throws Exception {
        var environment = new StandardEnvironment();
        var sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addFirst(new SystemEnvironmentPropertySource("testEnvironment", variables));
        for (var source : new YamlPropertySourceLoader().load("application", new FileSystemResource(yaml))) {
            sources.addLast(source);
        }
        return Binder.get(environment).bind("fire.device", Bindable.of(FireDeviceProperties.class)).get();
    }

    private static int availablePort() throws Exception {
        try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }

    private static FireTcpServer server(FireDeviceProperties properties) {
        // Connections send no data, so the persistence service is never called.
        return new FireTcpServer(properties, null, new FireConnections(properties));
    }

    private static void lifecycle(FireDeviceProperties properties, String name) throws Exception {
        var accepted = new java.util.concurrent.CountDownLatch(1);
        var connections = new FireConnections(properties) {
            @Override public void register(String session, Runnable close) {
                super.register(session, close);
                accepted.countDown();
            }
        };
        var tcp = new FireTcpServer(properties, null, connections);
        try {
            tcp.start();
            check(tcp.isRunning(), name + " starts");
            tcp.start();
            check(tcp.isRunning(), name + " start is idempotent");
            try (var client = new Socket()) {
                client.connect(new InetSocketAddress("127.0.0.1", properties.getPort()), 2000);
                check(client.isConnected(), name + " accepts real loopback connection");
                check(accepted.await(2, java.util.concurrent.TimeUnit.SECONDS), name + " dispatches session");
            }
        } finally {
            tcp.stop();
        }
        check(!tcp.isRunning(), name + " stops");
        try (var probe = new ServerSocket()) {
            probe.setReuseAddress(false);
            probe.bind(new InetSocketAddress(properties.getBindAddress(), properties.getPort()));
            check(probe.isBound(), name + " releases port");
        }
    }

    public static void main(String[] args) throws Exception {
        var defaults = configuration(args[0], Map.of());
        check(defaults.isEnabled() && defaults.getBindAddress().equals("0.0.0.0") && defaults.getPort() == 2000,
                "application defaults work without a machine-specific IP");
        check(new FireDeviceProperties().getBindAddress().equals("0.0.0.0"), "Java fallback is portable");
        // Keep actual socket tests on random ports, without occupying the device's port 2000.
        defaults.setPort(availablePort());
        lifecycle(defaults, "wildcard listener");

        int customPort = availablePort();
        var custom = configuration(args[0], Map.of("FIRE_DEVICE_ENABLED", "true",
                "FIRE_DEVICE_BIND", "127.0.0.1", "FIRE_DEVICE_PORT", String.valueOf(customPort)));
        check(custom.isEnabled() && custom.getBindAddress().equals("127.0.0.1") && custom.getPort() == customPort,
                "deployment environment overrides address and port");
        lifecycle(custom, "specific interface listener");

        var disabled = configuration(args[0], Map.of("FIRE_DEVICE_ENABLED", "false",
                "FIRE_DEVICE_PORT", String.valueOf(availablePort())));
        var stopped = server(disabled);
        try {
            stopped.start();
            check(!stopped.isRunning(), "environment can disable TCP");
            try (var probe = new ServerSocket(disabled.getPort())) {
                check(probe.isBound(), "disabled TCP leaves port available");
            }
        } finally {
            stopped.stop();
        }

        try (var occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            custom.setPort(occupied.getLocalPort());
            var conflict = server(custom);
            boolean failed = false;
            try {
                conflict.start();
            } catch (IllegalStateException error) {
                failed = error.getCause() instanceof BindException
                        && error.getMessage().contains("127.0.0.1:" + occupied.getLocalPort());
            } finally {
                conflict.stop();
            }
            check(failed && !conflict.isRunning(), "port conflict fails with address and original cause");
        }

        String unavailable = "192.0.2.1";
        check(NetworkInterface.getByInetAddress(InetAddress.getByName(unavailable)) == null,
                "test address is not assigned locally");
        custom.setBindAddress(unavailable);
        custom.setPort(availablePort());
        var invalid = server(custom);
        boolean failed = false;
        try {
            invalid.start();
        } catch (IllegalStateException error) {
            failed = error.getCause() instanceof BindException
                    && error.getMessage().contains(unavailable + ":" + custom.getPort())
                    && error.getMessage().contains("0.0.0.0");
        } finally {
            invalid.stop();
        }
        check(failed && !invalid.isRunning(), "unavailable interface fails with actionable diagnostics");
        System.out.println("FIRE_TCP_BINDING_OK checks=" + checks);
    }
}
