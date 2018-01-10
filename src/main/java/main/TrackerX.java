package main;

import lombok.SneakyThrows;
import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackerX {
    public Flux<ConnectResponse> request(Flux<ConnectRequest> connectRequestFlux) {
        return connectRequestFlux
                .map(this::makeRequest)
                .handle(this::sendRequest)
                .handle(this::receiveResponse)
                .map((byte[] response) -> new ConnectResponse(response));
    }

    @SneakyThrows
    private DatagramPacket makeRequest(ConnectRequest request) {
        InetAddress trackerIp = InetAddress.getByName(request.getIp());
        byte[] requestPacket = request.buildRequestPacket();
        return new DatagramPacket(requestPacket, requestPacket.length, trackerIp, request.getPort());
    }

    private void sendRequest(DatagramPacket request, SynchronousSink<DatagramSocket> response) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.send(request);
            response.next(clientSocket);

        } catch (IOException exception) {
            response.error(exception);
        }
    }

    private void receiveResponse(DatagramSocket trackerSocket, SynchronousSink<byte[]> response) {
        try {
            byte[] receiveData = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            trackerSocket.receive(receivePacket);
            response.next(receiveData);

        } catch (IOException exception) {
            response.error(exception);
        } finally {
            trackerSocket.close();
        }
    }


}
