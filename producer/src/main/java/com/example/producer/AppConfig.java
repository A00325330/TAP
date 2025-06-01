package com.example.producer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
public class AppConfig {
@Value("${NODE_ID}")
private int nodeId;

@Value("${NETWORK_ID}")
private int networkId;

@Value("${NETWORK_NAME}")
private String networkName;

@Value("${STREAMING_ENABLED:false}")
private boolean streamingEnabled;

    public int getNodeId() {
        return nodeId;
    }
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
    public int getNetworkId() {
        return networkId;
    }
    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }
    public String getNetworkName() {
        return networkName;
    }
    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }
    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }
}

