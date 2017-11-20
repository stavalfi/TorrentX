package main.peer;

import main.NaturalX;

public class HaveMessage extends Message {

    /**
     * The payload is the zero-based index
     * of a piece that has just been successfully
     * downloaded and verified via the hash.
     * @param pieceIndex
     */
    public HaveMessage(int pieceIndex) {
        super(5, 4, new NaturalX.Natural1(pieceIndex).buffer());
    }
}
