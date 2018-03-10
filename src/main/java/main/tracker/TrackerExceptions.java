package main.tracker;

import java.net.SocketTimeoutException;
import java.util.function.Predicate;

public class TrackerExceptions {
    public static Predicate<Throwable> communicationErrors = (Throwable throwable) ->
            // response's transaction-id is
            // not equal to the request's transaction-id. or response's action-number is
            // not equal to the request's action-number.
            throwable instanceof BadResponseException ||

                    throwable instanceof SocketTimeoutException;
}
