package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class CancelMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 8;

    public CancelMessage(Peer from, Peer to, int index, int begin, int length) {
        super(to, from, CancelMessage.length, messageId, ByteBuffer.allocate(12).putInt(index)
                .putInt(begin)
                .putInt(length).array());
    }

    public CancelMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "CancelMessage{} " + super.toString();
    }
}