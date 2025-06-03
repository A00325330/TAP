package com.example.containermanager;
@Entity
public class BaseStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int nodeId;
    private int networkId;
    private String networkName;
    private boolean streamingEnabled;

    // Getters and setters

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

