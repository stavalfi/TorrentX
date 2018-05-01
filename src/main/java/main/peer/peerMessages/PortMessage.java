package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class PortMessage extends PeerMessage {
    private static final int length = 3;
    private static final byte messageId = 9;

    private short listenPort;

    public PortMessage(Peer from, Peer to, short listenPort) {
        super(to, from);
        this.listenPort = listenPort;
    }

    @Override
    public byte getMessageId() {
        return messageId;
    }

    @Override
    public int getMessageLength() {
        return length;
    }

    @Override
    public byte[] getMessagePayload() {
        return ByteBuffer.allocate(2)
                .putShort(listenPort)
                .array();
    }

    public short getListenPort() {
        return listenPort;
    }

    @Override
    public String toString() {
        return "PortMessage{} " + super.toString();
    }
}
