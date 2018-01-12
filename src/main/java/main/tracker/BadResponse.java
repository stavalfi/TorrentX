package main.tracker;

public class BadResponse extends Exception{
    public BadResponse(String message) {
        super(message);
    }
}
