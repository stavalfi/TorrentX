package main.tracker.response;

import java.nio.ByteBuffer;

public class TrackerResponse {
    private final String ip;
    private final int port;

    public TrackerResponse(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
