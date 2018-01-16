package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class IsAliveMessage extends PeerMessage {
    private static final int length=0;
    private static final byte messageId=0;
    public IsAliveMessage(Peer from, Peer to) {
        super(from, to, length, messageId, ByteBuffer.allocate(0).array());
    }
    public IsAliveMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
