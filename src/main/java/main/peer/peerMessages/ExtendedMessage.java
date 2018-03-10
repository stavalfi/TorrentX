package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.PeersCommunicator;

public class ExtendedMessage extends PeerMessage {
    private static final byte messageId = 20;

    public ExtendedMessage(PeersCommunicator peersCommunicator, Peer from, Peer to) {
        super(peersCommunicator, to, from, 1, messageId, new byte[0]);
    }

    public ExtendedMessage(PeersCommunicator peersCommunicator,Peer from, Peer to, byte[] peerMessage) {
        super(peersCommunicator, to, peerMessage, from);
    }

    @Override
    public String toString() {
        return "ExtendedMessage{} " + super.toString();
    }
}