package com.steps;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentParser;
import com.utils.CreateTrackerRequests;
import com.utils.PeerMessageType;
import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.HexByteConverter;
import main.Peer;
import main.PeerCommunicator;
import main.TrackerCommunicator;
import main.peer.HandShake;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import org.junit.Assert;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MyStepdefs {

    private Torrent torrentFile;
    byte[] torrentInfoHashAsByteArray;
    private Map<String, Short> trackersConnectionInfo;

    private Map.Entry<String, Short> activeTracker;
    private final byte[] peerIdAsByteArray = "-AZ5750-TpkXttZLfpSH".getBytes();
    private final short tcpPortApplicationListenOn = 8091;

    private ConnectRequest connectRequest;
    private AnnounceRequest announceRequest;
    private ScrapeRequest scrapeRequest;

    private ConnectResponse connectResponse;
    private AnnounceResponse announceResponse;
    private ScrapeResponse scrapeResponse;

    private Peer activePeer;

    private HandShake handShakeRequest, handShakeResponse;

    @Given("^new torrent file: \"([^\"]*)\".$")
    public void newTorrentFile(String torrentFilePath) throws Throwable {
        String torrentFilesLocation = "src/test/resources/";
        this.torrentFile = TorrentParser.parseTorrent(torrentFilesLocation + torrentFilePath);
        this.torrentInfoHashAsByteArray = HexByteConverter.hexToByte(this.torrentFile.getInfo_hash());
    }

    @When("^application read trackers for this torrent.$")
    public void applicationReadTrackersForThisTorrent() throws Throwable {
        // tracker pattern example: udp://tracker.coppersurfer.tk:6969/announce
        String trackerPattern = "^udp://(.*):(\\d*)(.*)?$";
        this.trackersConnectionInfo = this.torrentFile.getAnnounceList()
                .stream()
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .collect(Collectors.toMap((Matcher matcher) -> matcher.group(1), (Matcher matcher) -> Short.parseShort(matcher.group(2))));
    }

    @Then("^tracker response with same transaction id.$")
    public void trackerResponseWithSameTransactionId() throws Throwable {
        if (this.connectRequest != null) {
            Assert.assertNotNull(this.connectResponse);
            Assert.assertEquals(this.connectRequest.getAction(), this.connectResponse.getAction());
            Assert.assertEquals(this.connectRequest.getTransactionId(), this.connectResponse.getTransactionId());
        }
        if (this.scrapeRequest != null) {
            Assert.assertNotNull(this.scrapeResponse);
            Assert.assertEquals(this.scrapeRequest.getAction(), this.scrapeResponse.getAction());
            Assert.assertEquals(this.scrapeRequest.getTransactionId(), this.scrapeResponse.getTransactionId());
        }
        if (this.announceRequest != null) {
            Assert.assertNotNull(this.announceResponse);
            Assert.assertEquals(this.announceRequest.getAction(), this.announceResponse.getAction());
            Assert.assertEquals(this.announceRequest.getTransactionId(), this.announceResponse.getTransactionId());
        }

    }

    @Then("^choose one tracker.$")
    public void chooseOneTracker() throws Throwable {
        assert this.trackersConnectionInfo.size() > 0;
        this.activeTracker = this.trackersConnectionInfo
                .entrySet()
                .stream()
                .findFirst()
                .get();
    }

    @When("^application send tracker-request: CONNECT.$")
    public void applicationSendTrackerRequestCONNECT() throws Throwable {
        // create and send a single demo request
        this.connectRequest = CreateTrackerRequests.getInstance().createConnectRequest();

        this.connectResponse = TrackerCommunicator.communicate(this.activeTracker.getKey(),
                activeTracker.getValue(),
                this.connectRequest);
    }

    @When("^application send tracker-request: ANNOUNCE.$")
    public void applicationSendRequestRequestANNOUNCE() throws Throwable {
        // create and send a single demo request
        assert this.connectResponse != null;
        this.announceRequest = CreateTrackerRequests.getInstance().createAnnounceRequest(
                this.connectResponse.getConnectionId(),
                this.torrentInfoHashAsByteArray,
                this.peerIdAsByteArray,
                this.tcpPortApplicationListenOn);

        this.announceResponse = TrackerCommunicator.communicate(this.activeTracker.getKey(),
                activeTracker.getValue(),
                this.announceRequest);
    }

    @When("^application send tracker-request: SCRAPE.$")
    public void applicationSendRequestRequestSCRAPE() throws Throwable {
        // create and send a single demo request
        assert this.connectResponse != null;
        assert this.announceResponse != null;
        this.scrapeRequest = CreateTrackerRequests.getInstance().createScrapeRequest(
                this.connectResponse.getConnectionId(),
                this.torrentInfoHashAsByteArray);

        this.scrapeResponse = TrackerCommunicator.communicate(this.activeTracker.getKey(),
                activeTracker.getValue(),
                this.scrapeRequest);
    }

    @Then("^choose one active peer to communicate with.$")
    public void chooseOnePeer() throws Throwable {
        this.handShakeRequest = new HandShake(this.torrentInfoHashAsByteArray, this.peerIdAsByteArray);
        this.activePeer = this.announceResponse.getPeers()
                .stream()
                .filter((Peer peer) -> tryConnectPeer(peer))
                .findFirst()
                .get();
    }

    private boolean tryConnectPeer(Peer peer) {
        try {
            this.handShakeResponse = PeerCommunicator.sendMessage(
                    peer.getIpAddress(),
                    peer.getTcpPort(),
                    this.handShakeRequest);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @When("^application send to peer a peer-request: \"([^\"]*)\".$")
    public void applicationSendToPeerAPeerRequest(PeerMessageType peerMessageType) throws Throwable {

    }

    @Then("^application receive from peer a peer-response: \"([^\"]*)\".$")
    public void applicationReceiveFromPeerAPeerResponse(PeerMessageType peerMessageType) throws Throwable {
        switch (peerMessageType) {
            case HANDSHAKE:
                Assert.assertNotNull(this.handShakeResponse);
                String torrentInfoHashFromRequest = HexByteConverter.byteToHex(this.handShakeRequest.getTorrentInfoHash());
                String torrentInfoHashFromResponse = HexByteConverter.byteToHex(this.handShakeResponse.getTorrentInfoHash());
                Assert.assertEquals(torrentInfoHashFromRequest, torrentInfoHashFromResponse);
                break;
        }
    }
}

