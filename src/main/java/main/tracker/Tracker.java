package main.tracker;

public class Tracker {
    private String connectionType;
    private String tracker;
    private int port;

    public Tracker(String connectionType, String tracker, int port) {
        this.connectionType = connectionType;
        this.tracker = tracker;
        this.port = port;
    }

    public Tracker(Tracker tracker) {
        this.connectionType = tracker.connectionType;
        this.tracker = tracker.tracker;
        this.port = tracker.port;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getTracker() {
        return tracker;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Tracker{" +
                "tracker='" + tracker + '\'' +
                ", port=" + port +
                '}';
    }
}
