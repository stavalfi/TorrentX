package com.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PeersContainingPiece {
    private int pieceIndex;
    private String peers;

    public PeersContainingPiece(int pieceIndex, String peers) {
        this.pieceIndex = pieceIndex;
        this.peers = peers;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public List<Integer> getPeers() {
        return Arrays.stream(peers.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
