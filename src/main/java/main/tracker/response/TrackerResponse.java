package main.tracker.response;

import main.tracker.Tracker;

public abstract class TrackerResponse {
    private Tracker tracker;
    private int actionNumber;
    private int transactionId;


    public TrackerResponse(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public String toString() {
        return "TrackerResponse{" +
                "tracker=" + tracker +
                ", actionNumber=" + actionNumber +
                ", transactionId=" + transactionId +
                '}';
    }

    public Tracker getTracker() {
        return tracker;
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
