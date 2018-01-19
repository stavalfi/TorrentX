package main.tracker;

import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class ConnectToTracker {
    public static Mono<? extends ConnectResponse> connect(String ip, int port) {
        int transactionId = 123456;
        ConnectRequest request = new ConnectRequest(ip, port, transactionId);
        Function<ByteBuffer, ConnectResponse> createResponse = (ByteBuffer response) ->
                new ConnectResponse(ip, port, response.array());

        return TrackerCommunication.communicate(request, createResponse);
    }
}
