package main.tracker;

import main.HexByteConverter;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ScrapeToTracker {
    public static Mono<? extends ScrapeResponse> scrape(ConnectResponse connectResponse, List<String> torrentHash) {

        List<byte[]> torrentsHashes = torrentHash.stream()
                .map(HexByteConverter::hexToByte)
                .collect(Collectors.toList());

        ScrapeRequest request = new ScrapeRequest(connectResponse.getIp(), connectResponse.getPort(),
                connectResponse.getConnectionId(), 123456, torrentsHashes);

        Function<ByteBuffer, ScrapeResponse> createResponse = (ByteBuffer response) ->
                new ScrapeResponse(connectResponse.getIp(), connectResponse.getPort(), response.array(), torrentsHashes);

        return TrackerCommunication.communicate(request, createResponse);


    }
}
