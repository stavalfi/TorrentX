package com.utils;

import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaderBuilder;
import main.file.system.FileSystemLink;
import main.file.system.FileSystemLinkImpl;
import main.file.system.allocator.AllocatorStore;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.Link;
import main.peer.peerMessages.*;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.state.tree.TorrentStatusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redux.store.Store;

import java.util.AbstractMap;

public class RemoteFakePeerCopyCat {
    private static Logger logger = LoggerFactory.getLogger(RemoteFakePeerCopyCat.class);

    private Link link;
    private Mono<TorrentDownloader> torrentDownloader$;
    private Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore;
    private AllocatorStore allocatorStore;

    public RemoteFakePeerCopyCat(Link link, String identifier, String fakePeerTorrentDownloadPath,
                                 AllocatorStore allocatorStore,
                                 EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$,
                                 FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this.allocatorStore = allocatorStore;
        logger.debug("start initialize " + identifier);
        this.torrentStatusStore = new Store<>(new TorrentStatusReducer(), TorrentStatusReducer.defaultTorrentState, identifier + " - TorrentStatus-Store");
        this.link = link;
        int pieceIndex = 1; // the app will ask for this piece so we save this piece. because this reason - don't change it.
        int pieceLength = link.getTorrentInfo().getPieceLength(pieceIndex);
        int begin = 0;
        @SuppressWarnings("UnnecessaryLocalVariable")
        int blockLength = pieceLength;

        ConnectableFlux<PieceMessage> fakePieceMessageToSave$ = this.torrentStatusStore.dispatch(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
                .flatMap(__ -> this.torrentStatusStore.dispatch(TorrentStatusAction.START_DOWNLOAD_SELF_RESOLVED))
                .flatMap(__ -> this.torrentStatusStore.dispatch(TorrentStatusAction.START_DOWNLOAD_WIND_UP))
                .flatMap(__ -> this.torrentStatusStore.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS))
                .flatMap(__ -> this.torrentStatusStore.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_SELF_RESOLVED))
                .flatMap(__ -> this.torrentStatusStore.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_WIND_UP))
                .flatMap(__ -> this.allocatorStore.updateAllocations(10, blockLength))
                .flatMap(allocatorState -> this.allocatorStore.createPieceMessage(link.getPeer(), link.getMe(), pieceIndex, begin, blockLength, allocatorState.getBlockLength()))
                .doOnNext(pieceMessageToSave -> {
                    for (int i = 0; i < blockLength; i++)
                        pieceMessageToSave.getAllocatedBlock().getBlock()[i] = 11;
                })
                .doOnNext(pieceMessage -> logger.debug(identifier + " - setup piece1: " + pieceMessage + "."))
                .flux()
                .publish();

        FileSystemLink fileSystemLink$ = new FileSystemLinkImpl(link.getTorrentInfo(), fakePeerTorrentDownloadPath, this.allocatorStore, this.torrentStatusStore, fakePieceMessageToSave$, "App");

        Mono<Integer> pieceSaved$ = fileSystemLink$.savedPieces$()
                .filter(savedPieceIndex -> savedPieceIndex.equals(pieceIndex))
                .doOnNext(__ -> logger.info(identifier + " finished to save the piece: " + pieceIndex))
                .replay(1)
                .autoConnect(0)
                .take(1)
                .single();
        fakePieceMessageToSave$.connect(); // let the FS receive the piece I gave him.

        // wait until the piece we gave to the FS is saved.
        this.torrentDownloader$ = pieceSaved$.flatMap(__ -> {
            // I'm using this object to send pieceMessages and Request messages to the real app.
            return TorrentDownloaderBuilder.builder(link.getTorrentInfo(), "Fake peer")
                    .setIncomingPeerMessages(incomingPeerMessages$)
                    .setEmitIncomingPeerMessages(emitIncomingPeerMessages)
                    .setAllocatorStore(this.allocatorStore)
                    .setTorrentStatusStore(this.torrentStatusStore)
                    .setToDefaultSearchPeers()
                    .setToDefaultTorrentStatesSideEffects()
                    .setToDefaultPeersCommunicatorFlux()
                    .setFileSystemLink(fileSystemLink$)
                    .setToDefaultBittorrentAlgorithm()
                    .build();
        })
                .doOnNext(__ -> logger.debug("end initialize " + identifier))
                .cache();

        StepVerifier.create(this.torrentDownloader$)
                .expectNextCount(1)
                .verifyComplete();

        this.torrentDownloader$.map(TorrentDownloader::getIncomingPeerMessagesNotifier)
                .flatMapMany(IncomingPeerMessagesNotifier::getIncomingPeerMessages$)
                .map(AbstractMap.SimpleEntry::getValue)
                .index()
                .doOnNext(peerMessage -> logger.debug("RemoteFakePeerCopyCat (" + identifier + ") received new (" + peerMessage.getT1() + ") message from app: " + peerMessage))
                .flatMap(peerMessage -> {
                    // the index start from zero.
                    if (peerMessage.getT1() == 1) {
//                        blockThread(0 * 1000);
                    }
                    if (peerMessage.getT1() == 2) {
                        link.closeConnection();
                        if (peerMessage.getT2() instanceof PieceMessage)
                            return allocatorStore.free(((PieceMessage) peerMessage.getT2()).getAllocatedBlock())
                                    .map(__ -> peerMessage.getT2())
                                    .ignoreElement();
                        return Mono.empty();
                    }
                    return Mono.just(peerMessage.getT2());
                })
                .flatMap(peerMessage -> responseToApp(link, peerMessage))
                .publish()
                .autoConnect(0);
    }

    private Mono<? extends PeerMessage> responseToApp(Link link, PeerMessage peerMessage) {
        if (peerMessage instanceof PieceMessage) {
            PieceMessage pieceMessage = (PieceMessage) peerMessage;
            return link.sendMessages().sendRequestMessage(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getLength())
                    .flatMap(__ -> this.allocatorStore.free(pieceMessage.getAllocatedBlock()))
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof RequestMessage) {
            RequestMessage requestMessage = (RequestMessage) peerMessage;
            return torrentDownloader$.map(TorrentDownloader::getFileSystemLink)
                    .doOnNext(__ -> logger.debug("start creating piece-message for response to app because he sent me request-message: " + requestMessage))
                    .flatMap(fileSystemLink -> fileSystemLink.buildPieceMessage(requestMessage))
                    .doOnNext(__ -> logger.debug("end creating piece-message for response to app because he sent me request-message: " + requestMessage))
                    .doOnNext(__ -> logger.debug("start send piece-message for response to app because he sent me request-message: " + requestMessage))
                    .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                            .doOnNext(__ -> logger.debug("end send piece-message for response to app because he sent me request-message. piece message: " + pieceMessage)))
                    .doOnError(error -> logger.error("wtf: " + error))
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof BitFieldMessage) {
            BitFieldMessage bitFieldMessage = (BitFieldMessage) peerMessage;
            return link.sendMessages()
                    .sendBitFieldMessage(bitFieldMessage.getPiecesStatus())
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof CancelMessage) {
            CancelMessage cancelMessage = (CancelMessage) peerMessage;
            return link.sendMessages().sendCancelMessage(cancelMessage.getIndex(),
                    cancelMessage.getBegin(),
                    cancelMessage.getBlockLength())
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof HaveMessage) {
            HaveMessage haveMessage = (HaveMessage) peerMessage;
            return link.sendMessages().sendHaveMessage(haveMessage.getPieceIndex())
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof InterestedMessage) {
            return link.sendMessages().sendInterestedMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof KeepAliveMessage) {
            return link.sendMessages().sendKeepAliveMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof NotInterestedMessage) {
            return link.sendMessages().sendNotInterestedMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof UnchokeMessage) {
            return link.sendMessages().sendUnchokeMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof PortMessage) {
            PortMessage portMessage = (PortMessage) peerMessage;
            return link.sendMessages().sendPortMessage(portMessage.getListenPort())
                    .map(__ -> peerMessage);
        }
        return Mono.error(new Exception("we should never be here." +
                " that means that the App send the fake-peer a" +
                " message that I don't cover in the cases."));
    }

    public void dispose() {
        //TODO: we also need to free resources from this.allocatorStore.
        this.link.closeConnection();
        // it is important to remove all files before I
        // try to remove the main folder for the fake-peer.
        this.torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS);
        this.torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS);

        this.torrentStatusStore.notifyWhen(TorrentStatusAction.REMOVE_FILES_WIND_UP, this.torrentStatusStore)
                .flatMap(store -> store.notifyWhen(TorrentStatusAction.REMOVE_TORRENT_WIND_UP))
                .defaultIfEmpty(TorrentStatusReducer.defaultTorrentState)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        Utils.deleteFakePeerDownloadFolder();
    }

    private void blockThread(int durationInMillis) {
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
