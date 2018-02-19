package main.tracker;

import main.tracker.requests.TrackerRequest;
import main.tracker.response.ErrorResponse;
import main.tracker.response.TrackerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.function.Function;

class TrackerCommunication {
    private static Logger logger = LoggerFactory.getLogger(TrackerCommunication.class);

    public static <Request extends TrackerRequest, Response extends TrackerResponse>
    Mono<Response> communicate(Request request, Function<ByteBuffer, Response> createResponse) {

        return sendRequest(request)
                // before we map to response bytes to response object, check if the response is ErrorResponse
                // by actionNumber. If yes, create error-signal, else forward the signal.
                .flatMap((DatagramSocket trackerSocket) ->
                        receiveResponse(trackerSocket)
                                .handle((ByteBuffer response, SynchronousSink<ByteBuffer> sink) -> {
                                    if (ErrorResponse.isErrorResponse(response.array())) {
                                        ErrorResponse errorResponse = new ErrorResponse(request.getTracker(), response.array());
                                        sink.error(new TrackerErrorResponseException(errorResponse));
                                    } else
                                        sink.next(response);
                                }))
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
                .doOnError(TrackerExceptions.communicationErrors, error ->
                        logger.warn("error signal: (the application will maybe try to send" +
                                " the request again to the same tracker)." +
                                "\nerror message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()))
                // the retry operation will run the first, ever created, publisher again
                // which is defined in sendRequest method.
                .retry(1, TrackerExceptions.communicationErrors)
                .doOnError(TrackerExceptions.communicationErrors, error ->
                        logger.warn("error signal: (the application retried to send" +
                                " a request to the same tracker again and failed)." +
                                "\nerror message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()))
                .doOnError(TrackerExceptions.communicationErrors.negate(), error ->
                        logger.error("error signal: (the application doesn't try to send" +
                                " a request again after this error)." +
                                "\nerror message: " + error.getMessage() + ".\n" +
                                "error type: " + error.getClass().getName()));
    }

    private static <Request extends TrackerRequest> Mono<DatagramSocket> sendRequest(Request request) {
        return makeRequest(request)
                .flatMap(TrackerCommunication::sendRequest);
    }

    private static <Request extends TrackerRequest> Mono<DatagramPacket> makeRequest(Request request) {
        return Mono.create(sink -> {
            try {
                InetAddress trackerIp = InetAddress.getByName(request.getTracker().getTracker());
                byte[] requestPacket = request.buildRequestPacket().array();
                DatagramPacket datagramPacket = new DatagramPacket(requestPacket, requestPacket.length, trackerIp, request.getTracker().getPort());
                sink.success(datagramPacket);
            } catch (UnknownHostException ex) {
                // copy the exception details and add the request information
                Exception error = new UnknownHostException(ex.getMessage() + ": " + request.toString());
                error.setStackTrace(ex.getStackTrace());
                sink.error(error);
            } catch (SecurityException ex) {
                // copy the exception details and add the request information
                Exception error = new SecurityException(ex.getMessage() + ": " + request.toString());
                error.setStackTrace(ex.getStackTrace());
                sink.error(error);
            }
        });
    }

    private static Mono<DatagramSocket> sendRequest(DatagramPacket request) {
        return Mono.create(sink -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                sink.onCancel(() -> clientSocket.close());
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
                sink.onCancel(() -> trackerSocket.close());
                byte[] receiveData = new byte[1000];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                trackerSocket.setSoTimeout(1000);
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
