package main.peer;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Predicate;

public class PeerExceptions {
    public static Predicate<Throwable> communicationErrors = (Throwable throwable) -> {
        boolean b = throwable instanceof NoRouteToHostException || // no route to host (Host unreachable)
                throwable instanceof ConnectException || // the peer did not accept the connection
                throwable instanceof SocketTimeoutException || // the peer is not available.
                throwable instanceof SocketException || // ??????????????????
                // Socket Closed and we tried to use it again.
                // i don't know how we can get this exception.
                throwable instanceof IOException ||
                throwable instanceof EOFException;
        return b; // the peer closed the connection while we read/wait for data from him.
    };

}
