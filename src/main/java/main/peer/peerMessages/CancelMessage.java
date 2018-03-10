package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.PeersCommunicator;

import java.nio.ByteBuffer;

public class CancelMessage extends PeerMessage {
    private static int length = 13;
    private static final byte messageId = 8;

    public CancelMessage(PeersCommunicator peersCommunicator, Peer from, Peer to, int index, int begin, int length) {
        super(peersCommunicator, to, from, CancelMessage.length, messageId, ByteBuffer.allocate(12).putInt(index)
                .putInt(begin)
                .putInt(length).array());
    }

    public CancelMessage(PeersCommunicator peersCommunicator,Peer from, Peer to, byte[] peerMessage) {
        super(peersCommunicator, to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "CancelMessage{} " + super.toString();
    }
}