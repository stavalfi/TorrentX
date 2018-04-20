package main.peer;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class PeerExceptions {
    public static Predicate<Throwable> communicationErrors = (Throwable throwable) -> {
        return throwable instanceof NoRouteToHostException || // no route to host (Host unreachable)
                throwable instanceof ConnectException || // the peer did not accept the connection
                throwable instanceof SocketTimeoutException || // the peer is not available.
                throwable instanceof SocketException || // socket is closed.
//                // Socket Closed and we tried to use it again.
//                // i don't know how we can get this exception.
//                throwable instanceof IOException ||
                // the peer closed the connection while we are
                // waiting for more incoming data in the stream.
                throwable instanceof EOFException;
    };

    // I requested a piece and he didn't response with the piece back or communication problems.
    public static Predicate<Throwable> peerNotResponding =
            communicationErrors.or(throwable -> throwable instanceof TimeoutException);


}
