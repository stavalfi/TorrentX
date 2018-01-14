package main.tracker;

public class Tracker {
    final String tracker;
    final int port;

    public Tracker(String tracker, int port) {
        this.tracker = tracker;
        this.port = port;
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
