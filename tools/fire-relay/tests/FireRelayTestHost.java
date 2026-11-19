import com.ruoyi.system.fire.*;
import com.ruoyi.system.mapper.FireAccessMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

/** Real production listeners and transactional mapper, exclusively on a disposable database. */
public class FireRelayTestHost {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:" + args[0]
                + "/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
        var ds = new DriverManagerDataSource(url, "root", "");
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setMapperLocations(new FileSystemResource(args[1]
                + "/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FireAccessMapper.xml"));
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FireAccessMapper.class);
        var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        context.register(org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class, FireIngestService.class);
        context.registerBean(FireAccessMapper.class, () -> mapper); context.refresh();
        var proxy = new TransactionProxyFactoryBean(); proxy.setTarget(context.getBean(FireIngestService.class));
        proxy.setTransactionManager(new DataSourceTransactionManager(ds)); proxy.setProxyTargetClass(true);
        var attributes = new Properties(); attributes.setProperty("*", "PROPAGATION_REQUIRED");
        proxy.setTransactionAttributes(attributes); proxy.afterPropertiesSet();
        var service = (FireIngestService) proxy.getObject();
        var devices = new FireDeviceProperties(); devices.setEnabled(true);
        devices.setBindAddress("127.0.0.1"); devices.setPort(Integer.parseInt(args[6])); devices.setIdleSeconds(5);
        var connections = new FireConnections(devices);
        var relay = new FireRelayProperties(); relay.setEnabled(true);
        relay.setBindAddress("127.0.0.1"); relay.setPort(Integer.parseInt(args[5]));
        relay.setKeyStore(args[2] + "/receiver.p12");
        relay.setKeyStorePasswordFile(args[2] + "/keystore-password.secret");
        relay.setSecretFile(args[2] + "/relay.secret"); relay.setNodeId("site-01");
        relay.setAllowedSourceCidrs(List.of("10.5.89.0/24", "10.5.90.0/24", "127.0.0.0/8"));
        relay.setMaxConnections(4); relay.setHandshakeSeconds(2);
        var receiver = new FireRelayServer(relay, devices, service, connections);
        var direct = new FireTcpServer(devices, service, connections);
        try {
            receiver.start(); direct.start();
            System.out.println("READY"); System.out.flush();
            var input = new BufferedReader(new InputStreamReader(System.in));
            String command;
            while ((command = input.readLine()) != null && !command.equals("STOP")) {
                if (command.equals("ONLINE")) {
                    System.out.println("ONLINE=" + connections.online("127.0.0.1")); System.out.flush();
                }
            }
        } finally { receiver.stop(); direct.stop(); context.close(); }
    }
}
