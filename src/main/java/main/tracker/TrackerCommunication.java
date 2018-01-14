package main.tracker;

import main.tracker.requests.TrackerRequest;
import main.tracker.response.ErrorResponse;
import main.tracker.response.TrackerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.function.Function;

class TrackerCommunication {

    public static <Request extends TrackerRequest, Response extends TrackerResponse>
    Mono<Response> communicate(Request request, Function<ByteBuffer, Response> createResponse) {

        return sendRequest(request)
                // before we map to response bytes to response object, check if the response is ErrorResponse
                // by actionNumber. If yes, create error-signal, else forward the signal.
                .handle((DatagramSocket trackerSocket, SynchronousSink<ByteBuffer> sink) -> {
                    getResponse(trackerSocket).subscribe(response -> {
                        if (ErrorResponse.isErrorResponse(response.array())) {
                            ErrorResponse errorResponse = new ErrorResponse(request.getIp(), request.getPort(), response.array());
                            sink.error(new TrackerErrorResponseException(errorResponse));
                        } else
                            sink.next(response);
                    }, sink::error, sink::complete);
                })
                .map(createResponse)
                .handle((Response response, SynchronousSink<Response> sink) -> {
                    if (request.getTransactionId() != response.getTransactionId() ||
                            request.getActionNumber() != response.getActionNumber())
                        sink.error(new BadResponseException("response's transaction-id is" +
                                " not equal to the request's transaction-id."));
                    if (request.getActionNumber() != response.getActionNumber())
                        sink.error(new BadResponseException("response's action-number is" +
                                " not equal to the request's action-number."));
                    sink.next(response);
                })
                // the retry operation will run the first, ever created, publisher again
                // which is defined in sendRequest method.
                .retry(2, exception -> exception instanceof BadResponseException ||
                        exception instanceof SocketTimeoutException);
    }

    private static <Request extends TrackerRequest>
    Mono<DatagramSocket> sendRequest(Request request) {
        return Mono.just(request)
                .flatMap(TrackerCommunication::makeRequest)
                .flatMap(TrackerCommunication::sendRequest);
    }

    private static Mono<ByteBuffer> getResponse(DatagramSocket trackerSocket) {
        return Mono.just(trackerSocket)
                .flatMap(TrackerCommunication::receiveResponse);
    }

    private static <Request extends TrackerRequest>
    Mono<DatagramPacket> makeRequest(Request request) {
        try {
            InetAddress trackerIp = InetAddress.getByName(request.getIp());
            byte[] requestPacket = request.buildRequestPacket().array();
            DatagramPacket datagramPacket = new DatagramPacket(requestPacket, requestPacket.length, trackerIp, request.getPort());
            return Mono.just(datagramPacket);
        } catch (UnknownHostException ex) {
            // copy the exception details and add the request information
            Exception error = new UnknownHostException(ex.getMessage() + ": " + request.toString());
            error.setStackTrace(ex.getStackTrace());
            return Mono.error(error);
        } catch (SecurityException ex) {
            // copy the exception details and add the request information
            Exception error = new SecurityException(ex.getMessage() + ": " + request.toString());
            error.setStackTrace(ex.getStackTrace());
            return Mono.error(error);
        }
    }

    private static Mono<DatagramSocket> sendRequest(DatagramPacket request) {
        return Mono.create(sink -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.send(request);
                sink.success(clientSocket);

            } catch (IOException exception) {
                sink.error(exception);
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
                ByteBuffer response = ByteBuffer.wrap(receiveData);
                sink.success(response);
            } catch (IOException exception) {
                sink.error(exception);
            } finally {
                trackerSocket.close();
            }
        });
    }

}
