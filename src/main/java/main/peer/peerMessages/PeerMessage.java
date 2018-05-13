package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.SendMessages;
import reactor.core.publisher.Mono;

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

    public Mono<SendMessages> sendMessage(SendMessages sendMessages) {
        return sendMessages.send(this);
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

    // TODO: remove it because we will have a unique method inside
    // SendMessages class for each message type and it will know
    // what is the message payload (if any) to send the message.
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
