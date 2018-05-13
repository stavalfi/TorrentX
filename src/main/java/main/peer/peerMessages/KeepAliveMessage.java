package main.peer.peerMessages;

import main.peer.Peer;

public class KeepAliveMessage extends PeerMessage {
    private static final int length = 0;
    private static final byte messageId = 10;

    public KeepAliveMessage(Peer from, Peer to) {
        super(to, from);
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
        return new byte[0];
    }

    @Override
    public String toString() {
        return "KeepAliveMessage{} " + super.toString();
    }
}