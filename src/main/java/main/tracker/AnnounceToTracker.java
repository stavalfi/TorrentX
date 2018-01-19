package main.tracker;

import main.AppConfig;
import main.HexByteConverter;
import main.tracker.requests.AnnounceRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class AnnounceToTracker {
    private static final int portWeListenForPeersRequests = AppConfig.getInstance().getTcpPortListeningForPeersMessages();
    private static final byte[] peerId = AppConfig.getInstance().getPeerId().getBytes();

    public static Mono<? extends AnnounceResponse> announce(ConnectResponse connectResponse, String torrentHash) {

        AnnounceRequest request = new AnnounceRequest(connectResponse.getIp(), connectResponse.getPort(), connectResponse.getConnectionId(),
                HexByteConverter.hexToByte(torrentHash), peerId, (short) portWeListenForPeersRequests);

        Function<ByteBuffer, AnnounceResponse> createResponse = (ByteBuffer response) ->
                new AnnounceResponse(connectResponse.getIp(), connectResponse.getPort(),
                        response.array(), request.getNumWant());

        return TrackerCommunication.communicate(request, createResponse);


    }
}
