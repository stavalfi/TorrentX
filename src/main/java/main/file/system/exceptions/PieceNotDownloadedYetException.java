package main.file.system.exceptions;

public class PieceNotDownloadedYetException extends Exception {
    public PieceNotDownloadedYetException(int pieceIndex) {
        super("piece requested but not yet downloaded. Piece index: " + pieceIndex);
    }
}
