package com.utils;

public class BlockOfPiece {
    private int pieceIndex;
    private int from;
    private Integer length; // if null, then it's until the end of the piece

    public BlockOfPiece(int pieceIndex, int from, Integer length) {
        this.pieceIndex = pieceIndex;
        this.from = from;
        this.length = length;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public int getFrom() {
        return from;
    }

    public Integer getLength() {
        return length;
    }
}
