package main.tracker;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

public class TrackerExceptions {
    public static Predicate<Throwable> communicationErrors = (Throwable throwable) ->
            // response's transaction-id is
            // not equal to the request's transaction-id. or response's action-number is
            // not equal to the request's action-number.
            throwable instanceof BadResponseException ||
                    // host is not reachable by it's url
                    throwable instanceof UnknownHostException ||
                    // Network is unreachable ????
                    throwable instanceof IOException ||
                    throwable instanceof SocketTimeoutException;
}
