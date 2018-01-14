package main.tracker;

public class BadResponseException extends Exception{
    public BadResponseException(String message) {
        super(message);
    }
}
