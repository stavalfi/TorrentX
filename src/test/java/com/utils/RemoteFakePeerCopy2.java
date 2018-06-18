package com.utils;

import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaderBuilder;
import main.downloader.TorrentDownloaders;
import main.file.system.FileSystemLink;
import main.file.system.FileSystemLinkImpl;
import main.peer.Link;
import main.peer.peerMessages.*;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.state.tree.TorrentStatusState;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redux.store.Store;

public class RemoteFakePeerCopy2 {
    private static Logger logger = LoggerFactory.getLogger(RemoteFakePeerCopy2.class);
    private Link link;
    private Mono<TorrentDownloader> torrentDownloader$;

    public RemoteFakePeerCopy2(Link link, String fakePeerTorrentDownloadPath) {
        this.link = link;
        int pieceIndex = 3;
        int pieceLength = link.getTorrentInfo().getPieceLength(pieceIndex);
        int begin = 0;
        int blockLength = pieceLength;

        this.torrentDownloader$ = createTorrentDownloader(link, fakePeerTorrentDownloadPath, pieceIndex, begin, blockLength);

        link.receivePeerMessages()
                .getPeerMessageResponseFlux()
                .index()
                .doOnNext(peerMessage -> logger.debug("RemoteFakePeerCopy2 received new message from app: " + peerMessage))
                .flatMap(peerMessage -> {
                    if (peerMessage.getT1() == 2)
                        blockThread(2000);
                    if (peerMessage.getT1() == 3) {
                        link.closeConnection();
                        return Mono.empty();
                    }
                    return Mono.just(peerMessage.getT2());
                })
                .flatMap(peerMessage -> responseToApp(link, peerMessage))
                .publish()
                .autoConnect(0);
    }

    private Publisher<? extends PeerMessage> responseToApp(Link link, PeerMessage peerMessage) {
        if (peerMessage instanceof PieceMessage) {
            PieceMessage pieceMessage = (PieceMessage) peerMessage;
            return link.sendMessages().sendRequestMessage(pieceMessage.getIndex(),
                    pieceMessage.getBegin(),
                    pieceMessage.getAllocatedBlock().getLength())
                    .flatMap(__ -> TorrentDownloaders.getAllocatorStore().free(pieceMessage.getAllocatedBlock()))
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof RequestMessage) {
            RequestMessage requestMessage = (RequestMessage) peerMessage;
            return torrentDownloader$.map(TorrentDownloader::getFileSystemLink)
                    .flatMap(fileSystemLink -> fileSystemLink.buildPieceMessage(requestMessage))
                    .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage))
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
            InterestedMessage interestedMessage = (InterestedMessage) peerMessage;
            return link.sendMessages().sendInterestedMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof KeepAliveMessage) {
            KeepAliveMessage keepAliveMessage = (KeepAliveMessage) peerMessage;
            return link.sendMessages().sendKeepAliveMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof NotInterestedMessage) {
            NotInterestedMessage notInterestedMessage = (NotInterestedMessage) peerMessage;
            return link.sendMessages().sendNotInterestedMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof UnchokeMessage) {
            UnchokeMessage unchokeMessage = (UnchokeMessage) peerMessage;
            return link.sendMessages().sendUnchokeMessage()
                    .map(__ -> peerMessage);
        }
        if (peerMessage instanceof PortMessage) {
            PortMessage portMessage = (PortMessage) peerMessage;
            return link.sendMessages().sendPortMessage(portMessage.getListenPort())
                    .map(__ -> peerMessage);
        }
        throw new RuntimeException("we should never be here." +
                " that means that the App send the fake-peer a" +
                " message that I don't cover in the cases.");
    }

    private Mono<TorrentDownloader> createTorrentDownloader(Link link, String fakePeerTorrentDownloadPath, int pieceIndex, int begin, int blockLength) {
        Flux<PieceMessage> fakePieceMessageToSave$ = TorrentDownloaders.getAllocatorStore()
                .updateAllocations(4, blockLength)
                .flatMap(allocatorState -> TorrentDownloaders.getAllocatorStore()
                        .createPieceMessage(link.getPeer(), link.getMe(), pieceIndex, begin, blockLength, allocatorState.getBlockLength()))
                .doOnNext(pieceMessageToSave -> {
                    for (int i = 0; i < blockLength; i++)
                        pieceMessageToSave.getAllocatedBlock().getBlock()[i] = 11;
                })
                .flux();

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState);

        Mono<FileSystemLink> fileSystemLink$ = FileSystemLinkImpl.create(link.getTorrentInfo(),
                fakePeerTorrentDownloadPath,
                torrentStatusStore,
                fakePieceMessageToSave$);

        // I'm using this object to send pieceMessages and Request messages to the real app.
        return TorrentDownloaderBuilder
                .builder(link.getTorrentInfo())
                .setTorrentStatusStore(torrentStatusStore)
                .setToDefaultSearchPeers()
                .setToDefaultTorrentStatesSideEffects()
                .setToDefaultPeersCommunicatorFlux()
                .setFileSystemLink$(fileSystemLink$)
                .build();
    }

    public void dispose() {
        this.link.closeConnection();
        // it is important to remove all files before I
        // try to remove the main folder for the fake-peer.
        this.torrentDownloader$.map(TorrentDownloader::getTorrentStatusStore)
                .flatMap(store -> store.dispatch(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS)
                        .flatMap(__ -> store.dispatch(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS)))
                .defaultIfEmpty(TorrentStatusReducer.defaultTorrentState)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        this.torrentDownloader$.map(TorrentDownloader::getTorrentStatusStore)
                .flatMap(store -> store.notifyWhen(TorrentStatusAction.REMOVE_FILES_WIND_UP, store))
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
