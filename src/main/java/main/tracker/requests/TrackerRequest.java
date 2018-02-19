package main.tracker.requests;

import main.tracker.Tracker;

import java.nio.ByteBuffer;

public abstract class TrackerRequest {
    private Tracker tracker;
    private final int actionNumber;
    private final int transactionId;


    public TrackerRequest(Tracker tracker, int actionNumber, int transactionId) {
        this.tracker = tracker;
        this.actionNumber = actionNumber;
        this.transactionId = transactionId;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public abstract ByteBuffer buildRequestPacket();

    @Override
    public String toString() {
        return "TrackerRequest{" +
                "tracker=" + tracker +
                ", actionNumber=" + actionNumber +
                ", transactionId=" + transactionId +
                '}';
    }
}
