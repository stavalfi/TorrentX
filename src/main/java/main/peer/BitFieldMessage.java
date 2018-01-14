package main.peer;

import java.util.BitSet;

public class BitFieldMessage extends Message {
    private static final byte messageId=5;
    /**
     * The bitField message may only be sent immediately after the handshaking
     *  sequence is completed, and before any other messages are sent.
     *  It is optional, and need not be sent if a client has no pieces.
     * @param peaces representing the pieces that have been successfully downloaded.
     *               The high bit in the first byte corresponds to piece index 0.
     *               Bits that are cleared indicated a missing piece,
     *               and set bits indicate a valid and available piece.
     *               Spare bits at the end are set to zero.
     */
    public BitFieldMessage(BitSet peaces) {
        super(1+peaces.toByteArray().length, messageId, peaces.toByteArray());
    }
}
