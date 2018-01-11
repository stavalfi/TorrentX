package main.tracker.requests;

public abstract class TrackerRequest<T> {
    private final String ip;
    private final int port;

    public TrackerRequest(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public abstract byte[] buildRequestPacket();

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
