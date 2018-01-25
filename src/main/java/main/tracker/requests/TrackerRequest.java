package main.tracker.requests;

import java.nio.ByteBuffer;

public abstract class TrackerRequest {
    private final String ip;
    private final int port;
    private final int actionNumber;
    private final int transactionId;


    public TrackerRequest(String ip, int port, int actionNumber, int transactionId) {
        this.ip = ip;
        this.port = port;
        this.actionNumber = actionNumber;
        this.transactionId = transactionId;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public abstract ByteBuffer buildRequestPacket();

    @Override
    public String toString() {
        return "TrackerRequest{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", actionNumber=" + actionNumber +
                ", transactionId=" + transactionId +
                '}';
    }
}
