package main.peer.peerMessages;

import main.peer.Peer;

public class ChokeMessage extends PeerMessage {
    private static final int length = 1;
    private static final byte messageId = 0;

    /**
     * The choke message is fixed-length and has no payload.
     */
    public ChokeMessage(Peer from, Peer to) {
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
        return "ChokeMessage{} " + super.toString();
    }
}