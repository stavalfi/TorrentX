package main.peer;

import main.Peer;

import java.nio.ByteBuffer;

public class PortMessage extends PeerMessage {
    private static final int length = 3;
    private static final byte messageId = 9;

    public PortMessage(Peer from, Peer to, short listenPort) {
        super(from, to, length, messageId, ByteBuffer.allocate(2).putShort(listenPort).array());
    }
    public PortMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
