package main.tracker;

import lombok.SneakyThrows;
import main.tracker.requests.TrackerRequest;
import main.tracker.response.TrackerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class TrackerX {

    public static <Request extends TrackerRequest>
    Mono<DatagramSocket> sendRequest(Request request) {
        return Mono.just(request)
                .map(TrackerX::makeRequest)
                .flatMap(TrackerX::sendRequest);
    }

    public static <Response extends TrackerResponse>
    Mono<Response> getResponse(DatagramSocket trackerSocket, Function<ByteBuffer, Response> createResponse) {
        return Mono.just(trackerSocket)
                .flatMap(TrackerX::receiveResponse)
                .map(createResponse);
    }

    @SneakyThrows
    private static <Request extends TrackerRequest>
    DatagramPacket makeRequest(Request request) {
        InetAddress trackerIp = InetAddress.getByName(request.getIp());
        byte[] requestPacket = request.buildRequestPacket().array();
        return new DatagramPacket(requestPacket, requestPacket.length, trackerIp, request.getPort());
    }

    private static Mono<DatagramSocket> sendRequest(DatagramPacket request) {
        return Mono.create(sink -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.send(request);
                sink.success(clientSocket);

            } catch (IOException exception) {
                sink.error(exception);
            } finally {
                sink.success();
            }
        });
    }

    private static Mono<ByteBuffer> receiveResponse(DatagramSocket trackerSocket) {
        return Mono.create(sink -> {
            try {
                byte[] receiveData = new byte[1000];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                trackerSocket.receive(receivePacket);
                sink.success(ByteBuffer.wrap(receiveData));
            } catch (IOException exception) {
                sink.error(exception);
            } finally {
                trackerSocket.close();
                sink.success();
            }
        });
    }


}
