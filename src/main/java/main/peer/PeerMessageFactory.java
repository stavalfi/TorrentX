package main.peer;

import main.TorrentInfo;
import main.downloader.TorrentDownloaders;
import main.file.system.allocator.AllocatorStore;
import main.peer.peerMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

// TODO: implement visitor.
public class PeerMessageFactory {
    private static Logger logger = LoggerFactory.getLogger(PeerMessageFactory.class);

    public static Mono<? extends PeerMessage> waitForMessage(AllocatorStore allocatorStore,
                                                             Scheduler scheduler,
                                                             TorrentInfo torrentInfo,
                                                             Peer from,
                                                             Peer to,
                                                             DataInputStream dataInputStream) {
        // lengthOfTheRestOfData == messageLength == how much do we need to read more
        int lengthOfTheRestOfData;
        try {
            lengthOfTheRestOfData = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        boolean amIApp = to.getPeerPort() == TorrentDownloaders.getListener().getTcpPort();
        String whoAmI = amIApp ? "App" : "Fake-peer";
        logger.debug(whoAmI + " start received message from: " + from + " to: " + to);

        if (lengthOfTheRestOfData == 0) {
            byte keepAliveMessageId = 10;
            PeerMessage peerMessage = createMessage(torrentInfo, from, to, keepAliveMessageId, new byte[0]);
            return Mono.just(peerMessage);
        }
        byte messageId;
        try {
            messageId = dataInputStream.readByte();
        } catch (IOException e) {
            return Mono.error(e);
        }
        logger.debug(whoAmI + " start received message-id: " + messageId +
                "==" + PeerMessageId.fromValue(messageId) + " from: " +
                from + " to: " + to);
        int messagePayloadLength = lengthOfTheRestOfData - 1;

        if (messageId == PeerMessageId.pieceMessage.getMessageId())
            return createPieceMessage(allocatorStore, scheduler,torrentInfo, from, to, messagePayloadLength, dataInputStream);

        byte[] messagePayloadByteArray = new byte[messagePayloadLength];
        try {
            dataInputStream.readFully(messagePayloadByteArray);
        } catch (IOException e) {
            return Mono.error(e);
        }

        if (messageId == PeerMessageId.requestMessage.getMessageId())
            return createRequestMessage(allocatorStore,scheduler, torrentInfo, from, to, messageId, messagePayloadByteArray);

        PeerMessage peerMessage = createMessage(torrentInfo, from, to, messageId, messagePayloadByteArray);
        logger.debug(whoAmI + " end received message-id: " + messageId +
                "==" + PeerMessageId.fromValue(messageId) + " from: " +
                from + " to: " + to);
        return Mono.just(peerMessage);
    }

    public static Mono<? extends PeerMessage> createPieceMessage(AllocatorStore allocatorStore,Scheduler scheduler,
                                                                 TorrentInfo torrentInfo, Peer from, Peer to,
                                                                 int messagePayloadLength, DataInputStream dataInputStream) {
        int index;
        try {
            index = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        int pieceLength = torrentInfo.getPieceLength(index);
        int begin;
        try {
            begin = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        int blockLength = messagePayloadLength - 8;// 8 == 'index' length in bytes + 'begin' length in bytes

        return allocatorStore.createPieceMessage(from, to, index, begin, blockLength, pieceLength)
                .publishOn(scheduler)
                .flatMap(pieceMessage -> {
                    try {
                        byte[] block = pieceMessage.getAllocatedBlock().getBlock();
                        int offset = pieceMessage.getAllocatedBlock().getOffset();
                        int length = pieceMessage.getAllocatedBlock().getLength();
                        dataInputStream.readFully(block, offset, length);
                        return Mono.just(pieceMessage);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })
                // TODO: remove the block() operator. we have a bug because
                // if we remove the block(), than sometimes I can't
                // readFully anything. Its like someone is reading
                // while I read.
                ;
    }

    public static Mono<? extends PeerMessage> createRequestMessage(AllocatorStore allocatorStore,
                                                                   Scheduler scheduler,
                                                                   TorrentInfo torrentInfo,
                                                                   Peer from,
                                                                   Peer to,
                                                                   byte messageId,
                                                                   byte[] payload) {
        assert messageId == PeerMessageId.requestMessage.getMessageId();

        ByteBuffer wrap = ByteBuffer.wrap(payload);
        int index = wrap.getInt();
        int begin = wrap.getInt();
        int blockLength = wrap.getInt();

        return allocatorStore.createRequestMessage(from, to, index, begin, blockLength,
                torrentInfo.getPieceLength(index))
                .publishOn(scheduler);
    }

    public static PeerMessage createMessage(TorrentInfo torrentInfo, Peer from, Peer to, byte messageId,
                                            byte[] payload) {
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
