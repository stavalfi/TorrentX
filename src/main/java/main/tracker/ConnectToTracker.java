package main.tracker;

import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Mono;

import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class ConnectToTracker {
    public static Mono<ConnectResponse> connect(String ip, int port) {
        int transactionId = 123456;
        ConnectRequest request = new ConnectRequest(ip, port, transactionId);
        Function<ByteBuffer, ConnectResponse> createResponse = (ByteBuffer response) ->
                new ConnectResponse(ip, port, response);

        return TrackerX.communicate(request, createResponse);
    }
}
