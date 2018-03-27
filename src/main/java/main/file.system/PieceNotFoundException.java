package main.file.system;

public class PieceNotFoundException extends Exception {
    public PieceNotFoundException(int pieceIndex) {
        super("piece requested but not found: " + pieceIndex);
    }
}
