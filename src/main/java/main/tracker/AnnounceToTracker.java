package main.tracker;

import main.HexByteConverter;
import main.tracker.requests.AnnounceRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class AnnounceToTracker {
    private static final short portWeListenForPeersRequests = 8181;
    private static final byte[] peerId = "-AZ5750-TpkXttZLfpSH".getBytes();

    public static Mono<AnnounceResponse> announce(ConnectResponse connectResponse, String torrentHash) {

        AnnounceRequest request = new AnnounceRequest(connectResponse.getIp(), connectResponse.getPort(), connectResponse.getConnectionId(),
                HexByteConverter.hexToByte(torrentHash), peerId, portWeListenForPeersRequests);

        Function<ByteBuffer, AnnounceResponse> createResponse = (ByteBuffer response) ->
                new AnnounceResponse(connectResponse.getIp(), connectResponse.getPort(),
                        response.array(), request.getNumWant());

        return TrackerCommunication.communicate(request, createResponse);


    }
}
