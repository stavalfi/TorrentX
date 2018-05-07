package main.peer;

import main.TorrentInfo;
import main.file.system.AllocatedBlock;
import main.file.system.AllocatorState;
import main.file.system.BlocksAllocatorImpl;
import main.peer.peerMessages.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

// TODO: implement visitor.
public class PeerMessageFactory {
    public static PeerMessage create(TorrentInfo torrentInfo, Peer from, Peer to, DataInputStream dataInputStream) throws IOException {
        final int messageLengthSize = 4;
        byte[] messageLengthSizeByteArray = new byte[messageLengthSize];
        dataInputStream.readFully(messageLengthSizeByteArray);
        // lengthOfTheRestOfData == messageLength == how much do we need to read more
        int lengthOfTheRestOfData = ByteBuffer.wrap(messageLengthSizeByteArray)
                .getInt();

        if (lengthOfTheRestOfData == 0) {
            byte keepAliveMessageId = 10;
            return create(torrentInfo, from, to, keepAliveMessageId, new byte[0]);
        }
        int messageIdLength = 1;
        byte[] messageIdByteArray = new byte[messageIdLength];
        dataInputStream.readFully(messageIdByteArray);

        int messagePayloadLength = lengthOfTheRestOfData - messageIdLength;
        if (messageIdByteArray[0] == PeerMessageId.pieceMessage.getMessageId())
            return createPieceMessage(torrentInfo, from, to, messagePayloadLength, dataInputStream);
        byte[] messagePayloadByteArray = new byte[messagePayloadLength];
        dataInputStream.readFully(messagePayloadByteArray);

        return create(torrentInfo, from, to, messageIdByteArray[0], messagePayloadByteArray);
    }

    public static PieceMessage createPieceMessage(TorrentInfo torrentInfo, Peer from, Peer to, int messagePayloadLength, DataInputStream dataInputStream) throws IOException {
        int index = dataInputStream.readInt();
        int pieceLength = torrentInfo.getPieceLength(index);
        int begin = PieceMessage.fixBlockBegin(pieceLength, dataInputStream.readInt());
        AllocatedBlock allocatedBlock = BlocksAllocatorImpl.getInstance()
                .getLatestState$()
                .map(AllocatorState::getBlockLength)
                .map(allocatedBlockLength -> PieceMessage.fixBlockLength(pieceLength, begin, messagePayloadLength - 8, allocatedBlockLength))
                .flatMap(fixedBlockLength -> BlocksAllocatorImpl.getInstance()
                        .allocate(0, fixedBlockLength))
                .block();

        dataInputStream.readFully(allocatedBlock.getBlock(), allocatedBlock.getOffset(), allocatedBlock.getLength());

        return new PieceMessage(from, to, index, begin, allocatedBlock, pieceLength);
    }

    public static PeerMessage create(TorrentInfo torrentInfo, Peer from, Peer to, byte messageId, byte[] payload) {
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
                RequestMessage requestMessage = new RequestMessage(from, to, index, begin, blockLength, torrentInfo.getPieceLength(index));
                return requestMessage;
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
