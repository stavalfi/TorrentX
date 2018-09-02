package main.peer;

import main.TorrentInfo;
import main.file.system.allocator.AllocatorStore;
import main.peer.peerMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Objects;

class ReceivePeerMessages {
    private static Logger logger = LoggerFactory.getLogger(ReceivePeerMessages.class);

    private Scheduler scheduler;

    public ReceivePeerMessages(AllocatorStore allocatorStore,
                               TorrentInfo torrentInfo, Peer me, Peer peer,
                               DataInputStream dataInputStream,
                               String identifier,
                               Link link,
                               FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this.scheduler = Schedulers.newSingle(identifier + " - MESSAGES");
        Mono<? extends PeerMessage> peerMessage$ = Mono.just(scheduler)
                .flatMap(__ -> waitForMessage(allocatorStore, scheduler, torrentInfo, peer, me, dataInputStream, identifier))
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                .subscribeOn(scheduler);

        Flux.from(peerMessage$)
                .doOnNext(peerMessage -> logger.debug(identifier + " - received new message: " + peerMessage))
                .doOnNext(peerMessage -> {
                    PeerMessageId peerMessageId = PeerMessageId.fromValue(peerMessage.getMessageId());
                    switch (peerMessageId) {
                        case bitFieldMessage:
                            link.getPeerCurrentStatus().updatePiecesStatus(((BitFieldMessage) peerMessage).getPiecesStatus());
                            break;
                        case haveMessage:
                            link.getPeerCurrentStatus().updatePiecesStatus(((HaveMessage) peerMessage).getPieceIndex());
                            break;
                        case interestedMessage:
                            link.getPeerCurrentStatus().setIsHeInterestedInMe(true);
                            break;
                        case notInterestedMessage:
                            link.getPeerCurrentStatus().setIsHeInterestedInMe(false);
                            break;
                        case chokeMessage:
                            link.getPeerCurrentStatus().setIsHeChokingMe(true);
                            break;
                        case unchokeMessage:
                            link.getPeerCurrentStatus().setIsHeChokingMe(false);
                            break;
                    }
                })
                .doOnNext(peerMessage -> emitIncomingPeerMessages.next(new AbstractMap.SimpleEntry<>(link, peerMessage)))
                .doOnError(throwable -> emitIncomingPeerMessages.error(throwable))
                .repeat()
                .publish()
                .autoConnect(0);
    }

    public void dispose() {
        this.scheduler.dispose();
    }

    private static Mono<? extends PeerMessage> waitForMessage(AllocatorStore allocatorStore, Scheduler scheduler, TorrentInfo torrentInfo,
                                                              Peer from, Peer to, DataInputStream dataInputStream, String identifier) {
        // lengthOfTheRestOfData == messageLength == how much do we need to read more
        int lengthOfTheRestOfData;
        try {
            lengthOfTheRestOfData = dataInputStream.readInt();
        } catch (IOException e) {
            return Mono.error(e);
        }
        logger.debug(identifier + " - start received message from: " + from + " to: " + to);

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
        logger.debug(identifier + " - start received message-id: " + messageId + "==" + PeerMessageId.fromValue(messageId) + " from: " + from + " to: " + to);
        int messagePayloadLength = lengthOfTheRestOfData - 1;

        if (messageId == PeerMessageId.pieceMessage.getMessageId())
            return createPieceMessage(allocatorStore, scheduler, torrentInfo, from, to, messagePayloadLength, dataInputStream, identifier);

        byte[] messagePayloadByteArray = new byte[messagePayloadLength];
        try {
            dataInputStream.readFully(messagePayloadByteArray);
        } catch (IOException e) {
            return Mono.error(e);
        }

        if (messageId == PeerMessageId.requestMessage.getMessageId())
            return createRequestMessage(allocatorStore, scheduler, torrentInfo, from, to, messageId, messagePayloadByteArray, identifier);

        PeerMessage peerMessage = createMessage(torrentInfo, from, to, messageId, messagePayloadByteArray);
        logger.debug(identifier + " - end received message-id: " + messageId +
                "==" + PeerMessageId.fromValue(messageId) + " from: " +
                from + " to: " + to);
        return Mono.just(peerMessage);
    }

    private static Mono<? extends PeerMessage> createPieceMessage(AllocatorStore allocatorStore, Scheduler scheduler,
                                                                  TorrentInfo torrentInfo, Peer from, Peer to,
                                                                  int messagePayloadLength, DataInputStream dataInputStream,
                                                                  String identifier) {
        int index;
        try {
            index = dataInputStream.readInt();
            logger.debug(identifier + " - start received piece message. piece-index: " + index);

        } catch (IOException e) {
            return Mono.error(e);
        }
        int pieceLength = torrentInfo.getPieceLength(index);
        int begin;
        try {
            begin = dataInputStream.readInt();
            logger.debug(identifier + " - start received piece message. piece-begin: " + begin);
        } catch (IOException e) {
            return Mono.error(e);
        }
        int blockLength = messagePayloadLength - 8;// 8 == 'index' length in bytes + 'begin' length in bytes
        logger.debug(identifier + " - start received piece message. piece-block-length: " + blockLength);
        return allocatorStore.createPieceMessage(from, to, index, begin, blockLength, pieceLength)
                .doOnNext(__ -> logger.debug(identifier + " - start received piece message. piece-index1: " + index))
                .publishOn(scheduler)
                .doOnNext(__ -> logger.debug(identifier + " - start received piece message. piece-index2: " + index))
                .flatMap(pieceMessage -> {
                    logger.debug(identifier + " - start received piece message. piece-block-length3: " + index);
                    try {
                        byte[] block = pieceMessage.getAllocatedBlock().getBlock();
                        int offset = pieceMessage.getAllocatedBlock().getOffset();
                        int length = pieceMessage.getAllocatedBlock().getLength();
                        dataInputStream.readFully(block, offset, length);
                        logger.debug(identifier + " - end received piece message: " + pieceMessage);
                        return Mono.just(pieceMessage);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }

    private static Mono<? extends PeerMessage> createRequestMessage(AllocatorStore allocatorStore,
                                                                    Scheduler scheduler,
                                                                    TorrentInfo torrentInfo,
                                                                    Peer from,
                                                                    Peer to,
                                                                    byte messageId,
                                                                    byte[] payload,
                                                                    String identifier) {
        assert messageId == PeerMessageId.requestMessage.getMessageId();

        ByteBuffer wrap = ByteBuffer.wrap(payload);
        int index = wrap.getInt();
        logger.debug(identifier + " - start received request message. request-index: " + index);
        int begin = wrap.getInt();
        logger.debug(identifier + " - start received request message. request-begin: " + begin);
        int blockLength = wrap.getInt();
        logger.debug(identifier + " - start received request message. request-block-length: " + blockLength);

        return allocatorStore.createRequestMessage(from, to, index, begin, blockLength, torrentInfo.getPieceLength(index))
                .doOnNext(__ -> logger.debug(identifier + " - end received request message."))
                .publishOn(scheduler);
    }

    private static PeerMessage createMessage(TorrentInfo torrentInfo, Peer from, Peer to, byte messageId,
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
