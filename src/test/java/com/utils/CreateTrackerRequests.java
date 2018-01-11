//package com.utils;
//
//import main.tracker.requests.AnnounceRequest;
//import main.tracker.requests.ConnectRequest;
//import main.tracker.requests.ScrapeRequest;
//
//import java.util.Arrays;
//
//public class CreateTrackerRequests {
//    public enum RequestType {
//        CONNECT,
//        ANNOUNCE,
//        SCRAPE
//    }
//
//    private static CreateTrackerRequests instance = new CreateTrackerRequests();
//
//    private CreateTrackerRequests() {
//
//    }
//
//    public static CreateTrackerRequests getInstance() {
//        return CreateTrackerRequests.instance;
//    }
//
//    public ConnectRequest createConnectRequest() {
//        int transactionId = 123456;
//        return new ConnectRequest(transactionId);
//    }
//
//    public AnnounceRequest createAnnounceRequest(long connectionId, byte[] torrentInfoHash,
//                                                 byte[] peerId, short tcpPortAppListeningOn) {
//        int transactionId = 123456;
//        return new AnnounceRequest(connectionId, transactionId, torrentInfoHash, peerId,
//                0, 0, 0, 2, 0, 0, 1000, tcpPortAppListeningOn);
//    }
//
//    public ScrapeRequest createScrapeRequest(long connectionId, byte[] torrentInfoHash) {
//        int transactionId = 123456;
//        return new ScrapeRequest(connectionId, transactionId, Arrays.asList(torrentInfoHash));
//    }
//
//}
