package main.tracker.requests;

import java.nio.ByteBuffer;

public abstract class TrackerRequest {
    private final String ip;
    private final int port;

    public TrackerRequest(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public abstract ByteBuffer buildRequestPacket();

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
