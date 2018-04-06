package main.peer;

import main.peer.peerMessages.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class PeerMessageFactory {
    public static PeerMessage create(Peer from, Peer to, DataInputStream dataInputStream) throws IOException {
        final int messageLengthSize = 4;
        byte[] data1 = new byte[messageLengthSize];
        dataInputStream.readFully(data1);
        int lengthOfTheRestOfData = ByteBuffer.wrap(data1).getInt(); // how much do we need to read more
        byte[] data2 = new byte[lengthOfTheRestOfData];

        dataInputStream.readFully(data2);

        ByteBuffer byteBuffer = ByteBuffer.allocate(messageLengthSize + lengthOfTheRestOfData);
        byteBuffer.putInt(lengthOfTheRestOfData);
        byteBuffer.put(data2);
        return create(from, to, byteBuffer.array()); // initialize message object from byte[]
    }

    public static PeerMessage create(Peer from, Peer to, byte[] peerMessage) {
        PeerMessageId messageId = PeerMessageId.fromValue(PeerMessage.getMessageId(peerMessage));
        switch (Objects.requireNonNull(messageId)) {
            case bitFieldMessage:
                return new BitFieldMessage(from, to, peerMessage);
            case cancelMessage:
                return new CancelMessage(from, to, peerMessage);
            case chokeMessage:
                return new ChokeMessage(from, to, peerMessage);
            case haveMessage:
                return new HaveMessage(from, to, peerMessage);
            case interestedMessage:
                return new InterestedMessage(from, to, peerMessage);
            case keepAliveMessage:
                return new KeepAliveMessage(from, to, peerMessage);
            case notInterestedMessage:
                return new NotInterestedMessage(from, to, peerMessage);
            case pieceMessage:
                return new PieceMessage(from, to, peerMessage);
            case portMessage:
                return new PortMessage(from, to, peerMessage);
            case requestMessage:
                return new RequestMessage(from, to, peerMessage);
            case unchokeMessage:
                return new UnchokeMessage(from, to, peerMessage);
            case extendedMessage:
                return new ExtendedMessage(from, to, peerMessage);
            default:
                throw new IllegalArgumentException("illegal message-id: " + messageId);
        }
    }
}
