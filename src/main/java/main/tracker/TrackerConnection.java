package main.tracker;

import main.AppConfig;
import main.HexByteConverter;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TrackerConnection extends Tracker {

    private final ConnectResponse connectResponse;

    public TrackerConnection(ConnectResponse connectResponse) {
        super(connectResponse.getTracker());
        this.connectResponse = connectResponse;
    }

    public Mono<? extends AnnounceResponse> announce(String torrentHash) {

        byte[] peerId = AppConfig.getInstance().getPeerId().getBytes();
        int portWeListenForPeersRequests = AppConfig.getInstance().getTcpPortListeningForPeersMessages();
        int howMuchPeersWeWant = 1000;

        AnnounceRequest announceRequest =
                new AnnounceRequest(this, this.connectResponse.getConnectionId(),
                        HexByteConverter.hexToByte(torrentHash), peerId, howMuchPeersWeWant, (short) portWeListenForPeersRequests);

        Function<ByteBuffer, AnnounceResponse> createResponse = (ByteBuffer response) ->
                new AnnounceResponse(this,
                        response.array(), howMuchPeersWeWant);

        return TrackerCommunication.communicate(announceRequest, createResponse);
    }

    public Mono<? extends ScrapeResponse> scrape(List<String> torrentHash) {

        List<byte[]> torrentsHashes = torrentHash.stream()
                .map(HexByteConverter::hexToByte)
                .collect(Collectors.toList());

        ScrapeRequest scrapeRequest =
                new ScrapeRequest(this, connectResponse.getConnectionId(), 123456, torrentsHashes);

        Function<ByteBuffer, ScrapeResponse> createResponse = (ByteBuffer response) ->
                new ScrapeResponse(this, response.array(), torrentsHashes);

        return TrackerCommunication.communicate(scrapeRequest, createResponse);
    }
}

