package main.peer;

import main.Peer;

import java.util.BitSet;

public class BitFieldMessage extends PeerMessage {
    private static final byte messageId = 5;

    /**
     * The bitField message may only be sent immediately after the handshaking
     * sequence is completed, and before any other messages are sent.
     * It is optional, and need not be sent if a client has no pieces.
     *
     * @param peaces representing the pieces that have been successfully downloaded.
     *               The high bit in the first byte corresponds to piece index 0.
     *               Bits that are cleared indicated a missing piece,
     *               and set bits indicate a valid and available piece.
     *               Spare bits at the end are set to zero.
     */
    public BitFieldMessage(Peer from, Peer to,BitSet peaces) {
        super(from, to, 1 + peaces.toByteArray().length, messageId, peaces.toByteArray());
    }

    public BitFieldMessage(Peer from, Peer to,byte[] peerMessage) {
        super(from, to, peerMessage);
    }
}
