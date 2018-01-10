package main.tracker.requests;

public abstract class TrackerRequest {
    private final String ip;
    private final int port;

    public TrackerRequest(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    abstract byte[] buildRequestPacket();

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
