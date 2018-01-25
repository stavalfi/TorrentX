package main.tracker;

import main.tracker.response.ErrorResponse;

public class TrackerErrorResponseException extends Exception {
    private final ErrorResponse errorResponse;

    public TrackerErrorResponseException(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
