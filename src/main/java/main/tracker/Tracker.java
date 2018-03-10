package main.tracker;

public class Tracker {
    private String connectionType;
    private String trackerUrl;
    private int udpPort;

    public Tracker(String connectionType, String trackerUrl, int udpPort) {
        this.connectionType = connectionType;
        this.trackerUrl = trackerUrl;
        this.udpPort = udpPort;
    }

    public Tracker(Tracker trackerUrl) {
        this.connectionType = trackerUrl.connectionType;
        this.trackerUrl = trackerUrl.trackerUrl;
        this.udpPort = trackerUrl.udpPort;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getTrackerUrl() {
        return trackerUrl;
    }

    public int getUdpPort() {
        return udpPort;
    }

    @Override
    public String toString() {
        return "Tracker{" +
                "trackerUrl='" + trackerUrl + '\'' +
                ", udpPort=" + udpPort +
                '}';
    }
}
