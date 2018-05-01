package main.peer.peerMessages;

import main.peer.Peer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ExtendedMessage extends PeerMessage {
    private static final byte messageId = 20;

    public ExtendedMessage(Peer from, Peer to) {
        super(to, from);
        throw new NotImplementedException();
    }

    @Override
    public byte getMessageId() {
        return messageId;
    }

    @Override
    public int getMessageLength() {
        return Integer.MAX_VALUE;
    }

    @Override
    public byte[] getMessagePayload() {
        return new byte[0];
    }

    @Override
    public String toString() {
        return "ExtendedMessage{} " + super.toString();
    }
}