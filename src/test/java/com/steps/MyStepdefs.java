package com.steps;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.tracker.AnnounceToTracker;
import main.tracker.ConnectToTracker;
import main.tracker.ScrapeToTracker;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import org.joou.UShort;
import org.junit.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.joou.Unsigned.ushort;

public class MyStepdefs {

    private Torrent torrent;

    private Flux<ConnectResponse> connectResponse;
    private Flux<AnnounceResponse> announceResponse;
    private Flux<ScrapeResponse> scrapeResponse;


    @Given("^new torrent file: \"([^\"]*)\".$")
    public void newTorrentFile(String torrentFilePath) throws Throwable {
        String torrentFilesLocation = "src/test/resources/";
        this.torrent = TorrentParser.parseTorrent(torrentFilesLocation + torrentFilePath);
    }

    @When("^application read trackers for this torrent.$")
    public void applicationReadTrackersForThisTorrent() throws Throwable {
        // tracker pattern example: udp://tracker.coppersurfer.tk:6969/scrape
        String trackerPattern = "^udp://(\\d*\\.)?(.*):(\\d*)(.*)?$";

        Stream<AbstractMap.SimpleEntry<String, UShort>> trackers = this.torrent.getAnnounceList()
                .stream()
                .filter((String tracker) -> !tracker.equals("udp://9.rarbg.com:2710/scrape")) // problematic tracker !!!!
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .map((Matcher matcher) -> new AbstractMap.SimpleEntry<String, UShort>(matcher.group(2), ushort(matcher.group(3))));

        this.connectResponse = Flux.fromStream(trackers)
                .flatMap(tracker -> ConnectToTracker.connect(tracker.getKey(), tracker.getValue().intValue()))
                .cache();

        this.announceResponse = this.connectResponse
                .flatMap(connectResponse -> AnnounceToTracker.announce(connectResponse, this.torrent.getInfo_hash()));

        this.scrapeResponse = this.connectResponse
                .flatMap(connectResponse -> ScrapeToTracker.scrape(connectResponse, Arrays.asList(this.torrent.getInfo_hash())));
    }

    @Then("^application send tracker-request: CONNECT.$")
    public void applicationSendTrackerRequestCONNECT() throws Throwable {
        this.connectResponse
                .subscribe((ConnectResponse connectResponse) -> {
                            Assert.assertEquals(0, connectResponse.getAction());
                        },
                        exception -> Assert.fail(exception.toString()));
    }

    @Then("^application send tracker-request: ANNOUNCE.$")
    public void applicationSendRequestRequestANNOUNCE() throws Throwable {
        this.announceResponse
                .subscribe((AnnounceResponse announceResponse) -> {
                            Assert.assertEquals(1, announceResponse.getAction());
                        },
                        exception -> Assert.fail(exception.toString()));
    }

    @Then("^application send tracker-request: SCRAPE.$")
    public void applicationSendRequestRequestSCRAPE() throws Throwable {
        this.scrapeResponse
                .subscribe((ScrapeResponse scrapeResponse) -> {
                            Assert.assertEquals(2, scrapeResponse.getAction());
                        },
                        exception -> Assert.fail(exception.toString()));
    }

//    @Then("^choose one active peer to communicate with.$")
//    public void chooseOnePeer() throws Throwable {
//        this.handShakeRequest = new HandShake(this.torrentInfoHashAsByteArray, this.peerIdAsByteArray);
//        this.activePeer = this.announceResponse.getPeers()
//                .stream()
//                .filter((Peer peer) -> tryConnectPeer(peer))
//                .findFirst()
//                .get();
//    }
//
//    private boolean tryConnectPeer(Peer peer) {
//        try {
//            this.handShakeResponse = PeerCommunicator.sendMessage(
//                    peer.getIpAddress(),
//                    peer.getTcpPort().intValue(),
//                    this.handShakeRequest);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @When("^application send to peer a peer-request: \"([^\"]*)\".$")
//    public void applicationSendToPeerAPeerRequest(PeerMessageType peerMessageType) throws Throwable {
//
//    }
//
//    @Then("^application receive from peer a peer-response: \"([^\"]*)\".$")
//    public void applicationReceiveFromPeerAPeerResponse(PeerMessageType peerMessageType) throws Throwable {
//        switch (peerMessageType) {
//            case HANDSHAKE:
//                Assert.assertNotNull(this.handShakeResponse);
//                String torrentInfoHashFromRequest = HexByteConverter.byteToHex(this.handShakeRequest.getTorrentInfoHash());
//                String torrentInfoHashFromResponse = HexByteConverter.byteToHex(this.handShakeResponse.getTorrentInfoHash());
//                Assert.assertEquals(torrentInfoHashFromRequest, torrentInfoHashFromResponse);
//                break;
//        }
//    }
}

