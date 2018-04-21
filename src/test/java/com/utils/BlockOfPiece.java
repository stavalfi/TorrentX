package com.utils;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockOfPiece)) return false;
        BlockOfPiece that = (BlockOfPiece) o;
        return getPieceIndex() == that.getPieceIndex() &&
                getFrom() == that.getFrom() &&
                Objects.equals(getLength(), that.getLength());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPieceIndex(), getFrom(), getLength());
    }

    @Override
    public String toString() {
        return "BlockOfPiece{" +
                "pieceIndex=" + pieceIndex +
                ", from=" + from +
                ", length=" + length +
                '}';
    }
}
