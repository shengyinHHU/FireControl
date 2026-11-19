package com.ruoyi.system.fire;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fire.relay")
public class FireRelayProperties {
    private boolean enabled;
    private String bindAddress = "127.0.0.1", nodeId = "site-01";
    private String keyStore = "", keyStorePasswordFile = "", secretFile = "";
    private int port = 22000, maxConnections = 64, handshakeSeconds = 5;
    private List<String> allowedSourceCidrs = List.of("10.5.89.0/24", "10.5.90.0/24");

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getBindAddress() { return bindAddress; }
    public void setBindAddress(String v) { bindAddress = v; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String v) { nodeId = v; }
    public String getKeyStore() { return keyStore; }
    public void setKeyStore(String v) { keyStore = v; }
    public String getKeyStorePasswordFile() { return keyStorePasswordFile; }
    public void setKeyStorePasswordFile(String v) { keyStorePasswordFile = v; }
    public String getSecretFile() { return secretFile; }
    public void setSecretFile(String v) { secretFile = v; }
    public int getPort() { return port; }
    public void setPort(int v) { port = v; }
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int v) { maxConnections = v; }
    public int getHandshakeSeconds() { return handshakeSeconds; }
    public void setHandshakeSeconds(int v) { handshakeSeconds = v; }
    public List<String> getAllowedSourceCidrs() { return allowedSourceCidrs; }
    public void setAllowedSourceCidrs(List<String> v) { allowedSourceCidrs = v; }
}
