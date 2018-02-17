package main.peer;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Predicate;

public class PeerExceptions {
    public static Predicate<Throwable> communicationErrors = (Throwable throwable) ->
            throwable instanceof NoRouteToHostException || // no route to host (Host unreachable)
                    throwable instanceof ConnectException || // the peer did not accept the connection
                    throwable instanceof SocketTimeoutException || // the peer is not available.
                    throwable instanceof SocketException || // ??????????????????
                    throwable instanceof EOFException; // the peer closed the connection while we read/wait for data from him.

}
