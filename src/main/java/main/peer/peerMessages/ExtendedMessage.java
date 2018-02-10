package main.peer.peerMessages;

import main.peer.Peer;

public class ExtendedMessage extends PeerMessage {
    private static final byte messageId = 20;

    public ExtendedMessage(Peer from, Peer to, byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
