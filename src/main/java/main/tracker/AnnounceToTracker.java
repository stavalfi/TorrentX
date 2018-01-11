package main.tracker;

import main.HexByteConverter;
import main.tracker.requests.AnnounceRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class AnnounceToTracker {
    static short portWeListenForPeersRequests = 8181;
    static byte[] peerId = "-AZ5750-TpkXttZLfpSH".getBytes();

    public static Mono<AnnounceResponse> announce(ConnectResponse connectResponse, String torrentHash) {

        AnnounceRequest announceRequest = new AnnounceRequest(connectResponse.getIp(), connectResponse.getPort(), connectResponse.getConnectionId(),
                HexByteConverter.hexToByte(torrentHash), peerId, portWeListenForPeersRequests);

        Function<ByteBuffer, AnnounceResponse> createResponse = (ByteBuffer response) ->
                new AnnounceResponse(connectResponse.getIp(), connectResponse.getPort(),
                        response, announceRequest.getNumWant());

        return TrackerX.sendRequest(announceRequest)
                .flatMap(socket -> TrackerX.getResponse(socket, createResponse));


    }
}
