package main.tracker;

import main.AppConfig;
import main.HexByteConverter;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrackerConnection extends Tracker {

    private final ConnectResponse connectResponse;

    public TrackerConnection(ConnectResponse connectResponse) {
        super(connectResponse.getTracker());
        this.connectResponse = connectResponse;
    }

    public Mono<? extends AnnounceResponse> announceMono(String torrentHash, int tcpPort) {

        byte[] peerId = AppConfig.getInstance().getPeerId().getBytes();
        int howMuchPeersWeWant = 1000;

        AnnounceRequest announceRequest =
                new AnnounceRequest(this, this.connectResponse.getConnectionId(),
                        HexByteConverter.hexToByte(torrentHash), peerId, howMuchPeersWeWant, (short) tcpPort);

        Function<ByteBuffer, AnnounceResponse> createResponse = (ByteBuffer response) ->
                new AnnounceResponse(this,
                        response.array(), howMuchPeersWeWant);

        return TrackerCommunication.communicateMono(announceRequest, createResponse);
    }

    public Mono<? extends ScrapeResponse> scrapeMono(List<String> torrentHash) {

        List<byte[]> torrentsHashes = torrentHash.stream()
                .map(HexByteConverter::hexToByte)
                .collect(Collectors.toList());

        ScrapeRequest scrapeRequest =
                new ScrapeRequest(this, connectResponse.getConnectionId(), 123456, torrentsHashes);

        Function<ByteBuffer, ScrapeResponse> createResponse = (ByteBuffer response) ->
                new ScrapeResponse(this, response.array(), torrentsHashes);

        return TrackerCommunication.communicateMono(scrapeRequest, createResponse);
    }

    @Override
    public String toString() {
        return "TrackerConnection{" +
                super.toString() +
                "} ";
    }
}

