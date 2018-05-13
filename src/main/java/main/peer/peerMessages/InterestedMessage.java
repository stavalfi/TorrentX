package main.peer.peerMessages;

import main.peer.Peer;

public class InterestedMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 2;

    /**
     * The interested message is fixed-length and has no payload.
     */
    public InterestedMessage(Peer from, Peer to) {
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
        return "InterestedMessage{} " + super.toString();
    }
}