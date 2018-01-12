package main.tracker;

import lombok.SneakyThrows;
import main.tracker.requests.TrackerRequest;
import main.tracker.response.TrackerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class TrackerCommunication {

    public static <Request extends TrackerRequest, Response extends TrackerResponse>
    Mono<Response> communicate(Request request, Function<ByteBuffer, Response> createResponse) {
        Mono<Response> responseMono = sendRequest(request)
                .flatMap((DatagramSocket trackerSocket) ->
                        getResponse(trackerSocket, createResponse));

        // the retry operation will run the first, ever created, publisher again
        // which is defined in sendRequest method.
        return responseMono.flatMap((Response response) -> {
            if (request.getTransactionId() != response.getTransactionId() ||
                    request.getActionNumber() != response.getActionNumber())
                return Mono.error(new BadResponse("response's transaction-id is not equal to the request's transaction-id."));
            if (request.getActionNumber() != response.getActionNumber())
                return Mono.error(new BadResponse("response's action-number is not equal to the request's action-number."));
            return responseMono;
        });
        //.retry(0, (Throwable exception) -> exception instanceof SocketTimeoutException || exception instanceof BadResponse);
    }

    private static <Request extends TrackerRequest>
    Mono<DatagramSocket> sendRequest(Request request) {
        return Mono.just(request)
                .map(TrackerCommunication::makeRequest)
                .flatMap(TrackerCommunication::sendRequest);
    }

    private static <Response extends TrackerResponse>
    Mono<Response> getResponse(DatagramSocket trackerSocket, Function<ByteBuffer, Response> createResponse) {
        return Mono.just(trackerSocket)
                .flatMap(TrackerCommunication::receiveResponse)
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
                trackerSocket.setSoTimeout(5000);
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
