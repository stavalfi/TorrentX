package main.peer.peerMessages;

import main.peer.Peer;

public class ExtendedMessage extends PeerMessage {
    private static final byte messageId = 20;

    public ExtendedMessage(Peer from, Peer to) {
        super(to, from, 1, messageId, new byte[0]);
    }

    public ExtendedMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
    }

    @Override
    public String toString() {
        return "ExtendedMessage{} " + super.toString();
    }
}