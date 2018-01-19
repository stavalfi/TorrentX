package com.utils;

import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import main.tracker.response.TrackerResponse;

public enum TrackerResponseType {
    Connect(ConnectResponse.class),
    Announce(AnnounceResponse.class),
    Scrape(ScrapeResponse.class);
    final Class<? extends TrackerResponse> signal;

    TrackerResponseType(Class<? extends TrackerResponse> signal) {
        this.signal = signal;
    }

    public Class<? extends TrackerResponse> getSignal() {
        return this.signal;
    }
}