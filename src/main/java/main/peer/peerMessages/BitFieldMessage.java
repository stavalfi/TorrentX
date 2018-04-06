package main.peer.peerMessages;

import main.peer.Peer;

import java.util.BitSet;

public class BitFieldMessage extends PeerMessage {
    private static final byte messageId = 5;
    private BitSet pieces;

    /**
     * The bitField message may only be sent immediately after the handshaking
     * sequence is completed, and before any other messages are sent.
     * It is optional, and need not be sent if a client has no pieces.
     *
     * @param pieces representing the pieces that have been successfully downloaded.
     *               The high bit in the first byte corresponds to piece index 0.
     *               Bits that are cleared indicated a missing piece,
     *               and set bits indicate a valid and available piece.
     *               Spare bits at the end are set to zero.
     */
    public BitFieldMessage(Peer from, Peer to, BitSet pieces) {
        super(to, from, 1 + pieces.toByteArray().length, messageId, pieces.toByteArray());
        this.pieces = pieces;
    }

    public BitFieldMessage(Peer from, Peer to, byte[] peerMessage) {
        super(to, peerMessage, from);
        this.pieces = BitSet.valueOf(super.getPayload());
    }


    @Override
    public String toString() {
        return "BitFieldMessage{} " + super.toString();
    }

    public BitSet getPiecesStatus() {
        return pieces;
    }
}
