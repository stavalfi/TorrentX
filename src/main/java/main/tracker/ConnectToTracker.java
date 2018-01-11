package main.tracker;

import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Function;

public class ConnectToTracker {
    public static Mono<ConnectResponse> connect(String ip, int port) {
        ConnectRequest request = new ConnectRequest(ip, port, 123456);
        Function<ByteBuffer, ConnectResponse> byteBufferConnectResponseFunction = (ByteBuffer response) -> new ConnectResponse(ip, port, response);

        return TrackerX.sendRequest(request)
                .flatMap((DatagramSocket trackerSocket) -> TrackerX.getResponse(trackerSocket, byteBufferConnectResponseFunction))
                .timeout(Duration.ofSeconds(1))
                .doOnError(System.out::println)
                .retry(1);
    }
}
