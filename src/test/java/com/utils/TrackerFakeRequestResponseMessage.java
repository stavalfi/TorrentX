package com.utils;

public class TrackerFakeRequestResponseMessage {
    private final TrackerRequestType trackerRequestType;
    private final ErrorSignalType errorSignalType;

    public TrackerFakeRequestResponseMessage(TrackerRequestType trackerRequestType, ErrorSignalType errorSignalType) {
        this.trackerRequestType = trackerRequestType;
        this.errorSignalType = errorSignalType;
    }

    public TrackerRequestType getTrackerRequestType() {
        return trackerRequestType;
    }

    public ErrorSignalType getErrorSignalType() {
        return errorSignalType;
    }
}
