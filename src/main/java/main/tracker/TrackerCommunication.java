package main.tracker;

import main.tracker.requests.TrackerRequest;
import main.tracker.response.ErrorResponse;
import main.tracker.response.TrackerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

class TrackerCommunication {
    private static Logger logger = LoggerFactory.getLogger(TrackerCommunication.class);

    public static <Request extends TrackerRequest, Response extends TrackerResponse>
    Mono<Response> communicate(Request request, Function<ByteBuffer, Response> createResponse) {

        Predicate<Throwable> retryOnErrors = (Throwable exception) -> exception instanceof BadResponseException ||
                exception instanceof SocketTimeoutException;

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
                .doOnError(retryOnErrors, error -> logger.warn("error signal: (the application maybe try to send the request again). ", error))
                // the retry operation will run the first, ever created, publisher again
                // which is defined in sendRequest method.
                .retry(1, retryOnErrors)
                .doOnError(retryOnErrors, error -> logger.warn("error signal: " +
                        "(the application retried to send a request again and failed). ", error))
                .doOnError(retryOnErrors.negate(), error -> logger.error("error signal: " +
                        "(the application didn't try to send a request again after this error). ", error))
                .log(null, Level.INFO, SignalType.ON_NEXT);
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
