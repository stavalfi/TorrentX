package main.tracker.response;

public abstract class TrackerResponse {
    private final String ip;
    private final int port;
    private int actionNumber;
    private int transactionId;


    public TrackerResponse(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "TrackerResponse{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", actionNumber=" + actionNumber +
                ", transactionId=" + transactionId +
                '}';
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    protected void setActionNumber(int actionNumber) {
        this.actionNumber = actionNumber;
    }

    public long getTransactionId() {
        return transactionId;
    }

    protected void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
}
