package com.steps;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentParser;
import com.utils.CreateTrackerRequests;
import com.utils.CreateTrackerRequests.RequestType;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.HexByteConverter;
import main.TrackerCommunicator;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import org.junit.Assert;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MyStepdefs {

    private Torrent torrentFile;
    private Map<String, Short> trackersConnectionInfo;

    private Map.Entry<String, Short> activeTrackerWeCommunicate;
    private final byte[] peerId = "-AZ5750-TpkXttZLfpSH".getBytes();
    private final short tcpPortApplicationListenOn = 8091;

    private ConnectRequest connectRequest;
    private AnnounceRequest announceRequest;
    private ScrapeRequest scrapeRequest;

    private ConnectResponse connectResponse;
    private AnnounceResponse announceResponse;
    private ScrapeResponse scrapeResponse;

    @Given("^new torrent file: \"([^\"]*)\"$")
    public void newTorrentFile(String torrentFilePath) throws Throwable {
        String torrentFilesLocation = "src/test/resources/";
        this.torrentFile = TorrentParser.parseTorrent(torrentFilesLocation + torrentFilePath);
    }

    @When("^application read trackers for this torrent$")
    public void applicationReadTrackersForThisTorrent() throws Throwable {
        // tracker pattern example: udp://tracker.coppersurfer.tk:6969/announce
        String trackerPattern = "^udp://(.*):(\\d*)(.*)?$";
        this.trackersConnectionInfo = this.torrentFile.getAnnounceList()
                .stream()
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .collect(Collectors.toMap((Matcher matcher) -> matcher.group(1), (Matcher matcher) -> Short.parseShort(matcher.group(2))));
    }

    @When("^application send request: \"([^\"]*)\"$")
    public void applicationSendRequest(RequestType requestType) throws Throwable {
        byte[] torrentInfoHashAsByteArray = HexByteConverter.hexToByte(this.torrentFile.getInfo_hash());

        // create and send a single demo request
        CreateTrackerRequests requests = CreateTrackerRequests.getInstance();
        switch (requestType) {
            case CONNECT:
                this.connectRequest = requests.createConnectRequest();
                this.activeTrackerWeCommunicate = this.trackersConnectionInfo
                        .entrySet()
                        .stream()
                        .findFirst()
                        .get();
                this.connectResponse = TrackerCommunicator.communicate(this.activeTrackerWeCommunicate.getKey(),
                        activeTrackerWeCommunicate.getValue(),
                        this.connectRequest);
                break;
            case ANNOUNCE:
                assert this.connectResponse != null;
                this.announceRequest = requests.createAnnounceRequest(this.connectResponse.getConnectionId(), torrentInfoHashAsByteArray,
                        this.peerId, this.tcpPortApplicationListenOn);

                this.announceResponse = TrackerCommunicator.communicate(this.activeTrackerWeCommunicate.getKey(),
                        activeTrackerWeCommunicate.getValue(),
                        this.announceRequest);
                break;
            case SCRAPE:
                assert this.connectResponse != null;
                assert this.announceResponse != null;
                this.scrapeRequest = requests.createScrapeRequest(this.connectResponse.getConnectionId(), torrentInfoHashAsByteArray);
                this.scrapeResponse = TrackerCommunicator.communicate(this.activeTrackerWeCommunicate.getKey(),
                        activeTrackerWeCommunicate.getValue(),
                        this.scrapeRequest);
                break;
        }
    }

    @Then("^tracker response with same transaction id$")
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
}

