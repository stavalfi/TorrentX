package main;

import lombok.SneakyThrows;
import main.tracker.requests.TrackerRequest;
import main.tracker.response.TrackerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class TrackerX {

    public static <Message, Request extends TrackerRequest<Message>, Response extends TrackerResponse<Message>> Flux<Response>
    request(Request request, Function<ByteBuffer, Response> createResponse) {
        return Flux.just(request)
                .map(TrackerX::makeRequest)
                .handle(TrackerX::sendRequest)
                .handle(TrackerX::receiveResponse)
                .map(createResponse);
    }

    @SneakyThrows
    private static <Message,Request extends TrackerRequest<Message>> DatagramPacket makeRequest(Request request) {
        InetAddress trackerIp = InetAddress.getByName(request.getIp());
        byte[] requestPacket = request.buildRequestPacket();
        return new DatagramPacket(requestPacket, requestPacket.length, trackerIp, request.getPort());
    }

    private static void sendRequest(DatagramPacket request, SynchronousSink<DatagramSocket> response) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.send(request);
            response.next(clientSocket);

        } catch (IOException exception) {
            response.error(exception);
        }
    }

    private static void receiveResponse(DatagramSocket trackerSocket, SynchronousSink<ByteBuffer> response) {
        try {
            byte[] receiveData = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            trackerSocket.receive(receivePacket);
            response.next(ByteBuffer.wrap(receiveData));

        } catch (IOException exception) {
            response.error(exception);
        } finally {
            trackerSocket.close();
        }
    }


}
