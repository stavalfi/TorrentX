package main.peer;

import main.peer.peerMessages.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

public class PeerMessageFactory {
    public static PeerMessage create(Peer from, Peer to, DataInputStream dataInputStream) throws IOException {
        final int messageLengthSize = 4;
        byte[] messageLengthSizeByteArray = new byte[messageLengthSize];
        dataInputStream.readFully(messageLengthSizeByteArray);
        // lengthOfTheRestOfData == messageLength == // how much do we need to read more
        int lengthOfTheRestOfData = ByteBuffer.wrap(messageLengthSizeByteArray)
                .getInt();

        if (lengthOfTheRestOfData == 0) {
            byte keepAliveMessageId = 10;
            return create(from, to, keepAliveMessageId, new byte[0]);
        }
        int messageIdLength = 1;
        byte[] messageIdByteArray = new byte[1];
        dataInputStream.readFully(messageIdByteArray);

        int messagePayloadLength = lengthOfTheRestOfData - messageIdLength;
        byte[] messagePayloadByteArray = new byte[messagePayloadLength];
        dataInputStream.readFully(messagePayloadByteArray);

        return create(from, to, messageIdByteArray[0], messagePayloadByteArray);
    }

    public static PeerMessage create(Peer from, Peer to, byte messageId, byte[] payload) {
        PeerMessageId peerMessageId = PeerMessageId.fromValue(messageId);
        switch (Objects.requireNonNull(peerMessageId)) {
            case bitFieldMessage:
                return new BitFieldMessage(from, to, BitSet.valueOf(payload));
            case cancelMessage: {
                ByteBuffer wrap = ByteBuffer.wrap(payload);
                int index = wrap.getInt();
                int begin = wrap.getInt();
                int blockLength = wrap.getInt();
                return new CancelMessage(from, to, index, begin, blockLength);
            }
            case chokeMessage:
                return new ChokeMessage(from, to);
            case haveMessage:
                int pieceIndex = ByteBuffer.wrap(payload).getInt();
                return new HaveMessage(from, to, pieceIndex);
            case interestedMessage:
                return new InterestedMessage(from, to);
            case keepAliveMessage:
                return new KeepAliveMessage(from, to);
            case notInterestedMessage:
                return new NotInterestedMessage(from, to);
            case pieceMessage: {
                ByteBuffer wrap = ByteBuffer.wrap(payload);
                int index = wrap.getInt();
                int begin = wrap.getInt();
                byte[] block = new byte[payload.length - 8];
                wrap.get(block);
                return new PieceMessage(from, to, index, begin, block);
            }
            case portMessage: {
                ByteBuffer wrap = ByteBuffer.wrap(payload);
                short portNumber = wrap.getShort();
                return new PortMessage(from, to, portNumber);
            }
            case requestMessage: {
                ByteBuffer wrap = ByteBuffer.wrap(payload);
                int index = wrap.getInt();
                int begin = wrap.getInt();
                int blockLength = wrap.getInt();
                return new RequestMessage(from, to, index, begin, blockLength);
            }
            case unchokeMessage:
                return new UnchokeMessage(from, to);
            case extendedMessage:
                return new ExtendedMessage(from, to);
            default:
                throw new IllegalArgumentException("illegal message-id: " + messageId);
        }
    }
}
