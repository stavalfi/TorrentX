package main.peer.exceptions;

public class BadTorrentInfoHashHandShakeException extends Exception {

    public BadTorrentInfoHashHandShakeException(String message) {
        super(message);
    }
}
