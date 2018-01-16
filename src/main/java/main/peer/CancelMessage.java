package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class CancelMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 7;

    public CancelMessage(Peer from, Peer to, int index, int begin, int length) {
        super(from, to, length, messageId, ByteBuffer.allocate(12).putInt(index)
                .putInt(begin)
                .putInt(length).array());
    }

    public CancelMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
