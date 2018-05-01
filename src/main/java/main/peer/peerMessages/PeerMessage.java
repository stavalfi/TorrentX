package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public abstract class PeerMessage implements Comparable<PeerMessage> {

    private final Peer from;
    private final Peer to;
    //    private final int length; // 4 bytes - the length in bytes of sizeof(messageId) + sizeof(payload)
//    private final byte messageId; // 1 byte

    PeerMessage(Peer to, Peer from) {
        this.from = from;
        this.to = to;
    }

    public byte[] createPacketFromObject() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + getMessageLength());

        buffer.putInt(getMessageLength());
        // when receiving a peerMessage,
        // I first check what is the value of "length".
        // if length == 0 then I don't read any more bytes.
        // so there is no reason to send dummy bytes.
        if (getMessageLength() > 0) {
            buffer.put(getMessageId());
            buffer.put(getMessagePayload());
        }

        return buffer.array();
    }

    public static int getMessageId(byte[] peerMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(peerMessage);
        int length = buffer.getInt();
        if (length == 0)
            return 10; // it's KeepAlive message;
        return buffer.get();
    }

    public abstract byte getMessageId();

    public abstract int getMessageLength();

    public abstract byte[] getMessagePayload();

    public Peer getFrom() {
        return from;
    }

    public Peer getTo() {
        return to;
    }

    @Override
    public int compareTo(PeerMessage peerMessage) {
        if (getMessageId() > peerMessage.getMessageId())
            return 1;
        else if (getMessageId() < peerMessage.getMessageId())
            return -1;
        return 0;
    }

    @Override
    public String toString() {
        return "PeerMessage{" +
                "from=" + from +
                ", to=" + to +
                ", messageId=" + getMessageId() +
                '}';
    }
}
