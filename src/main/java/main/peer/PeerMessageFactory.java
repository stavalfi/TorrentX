package main.peer;

import main.TorrentInfo;
import main.file.system.AllocatorState;
import main.file.system.BlocksAllocatorImpl;
import main.peer.peerMessages.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

// TODO: implement visitor.
public class PeerMessageFactory {
    public static Mono<? extends PeerMessage> waitForMessage(TorrentInfo torrentInfo, Peer from, Peer to, DataInputStream dataInputStream) {
        final int messageLengthSize = 4;
        byte[] messageLengthSizeByteArray = new byte[messageLengthSize];
        try {
            dataInputStream.readFully(messageLengthSizeByteArray);
        } catch (IOException e) {
            return Mono.error(e);
        }

        // I received something!
        // I will get the latest allocatedBlockSize.
        Integer allocatedBlockSize = BlocksAllocatorImpl.getInstance()
                .getLatestState$()
                .map(AllocatorState::getBlockLength)
                .publishOn(Schedulers.elastic())
                .block();

        // lengthOfTheRestOfData == messageLength == how much do we need to read more
        int lengthOfTheRestOfData = ByteBuffer.wrap(messageLengthSizeByteArray)
                .getInt();

        if (lengthOfTheRestOfData == 0) {
            byte keepAliveMessageId = 10;
            PeerMessage peerMessage = createMessage(torrentInfo, from, to, keepAliveMessageId, new byte[0], allocatedBlockSize);
            return Mono.just(peerMessage);
        }
        int messageIdLength = 1;
        byte[] messageIdByteArray = new byte[messageIdLength];
        try {
            dataInputStream.readFully(messageIdByteArray);
        } catch (IOException e) {
            return Mono.error(e);
        }

        int messagePayloadLength = lengthOfTheRestOfData - messageIdLength;
        if (messageIdByteArray[0] == PeerMessageId.pieceMessage.getMessageId())
            return createPieceMessage(torrentInfo, from, to, messagePayloadLength, dataInputStream);
        byte[] messagePayloadByteArray = new byte[messagePayloadLength];
        try {
            dataInputStream.readFully(messagePayloadByteArray);
        } catch (IOException e) {
            return Mono.error(e);
        }
        if (messageIdByteArray[0] == PeerMessageId.requestMessage.getMessageId())
            return createRequestMessage(torrentInfo, from, to, messageIdByteArray[0],
                    messagePayloadByteArray, allocatedBlockSize);

        PeerMessage peerMessage = createMessage(torrentInfo, from, to, messageIdByteArray[0],
                messagePayloadByteArray, allocatedBlockSize);
        return Mono.just(peerMessage);
    }

    public static Mono<? extends PeerMessage> createPieceMessage(TorrentInfo torrentInfo, Peer from, Peer to,
                                                                 int messagePayloadLength, DataInputStream dataInputStream) {
        int index = 0;
        try {
            index = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        int pieceLength = torrentInfo.getPieceLength(index);
        int begin = 0;
        try {
            begin = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        int blockLength = 0;
        try {
            blockLength = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        return BlocksAllocatorImpl.getInstance()
                .createPieceMessage(from, to, index, begin, blockLength, pieceLength)
                .flatMap(pieceMessage -> {
                    try {
                        dataInputStream.readFully(pieceMessage.getAllocatedBlock().getBlock(),
                                pieceMessage.getAllocatedBlock().getOffset(),
                                pieceMessage.getAllocatedBlock().getLength());
                        return Mono.just(pieceMessage);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }

    public static Mono<? extends PeerMessage> createRequestMessage(TorrentInfo torrentInfo, Peer from, Peer to, byte messageId,
                                                                   byte[] payload, int allocatedBlockSize) {
        assert messageId == PeerMessageId.requestMessage.getMessageId();

        ByteBuffer wrap = ByteBuffer.wrap(payload);
        int index = wrap.getInt();
        int begin = wrap.getInt();
        int blockLength = wrap.getInt();

        return BlocksAllocatorImpl.getInstance()
                .createRequestMessage(from, to, index, begin, blockLength,
                        torrentInfo.getPieceLength(index));
    }

    public static PeerMessage createMessage(TorrentInfo torrentInfo, Peer from, Peer to, byte messageId,
                                            byte[] payload, int allocatedBlockSize) {
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
            case unchokeMessage:
                return new UnchokeMessage(from, to);
            case extendedMessage:
                return new ExtendedMessage(from, to);
            default:
                throw new IllegalArgumentException("illegal message-id: " + messageId);
        }
    }
}
