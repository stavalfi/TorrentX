package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private Flux<TorrentPieceChanged> startDownloadFlux = Flux.empty();

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   TorrentStatus torrentStatus,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {

        uploadFlux(torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish()
                .connect();


    }

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    private Flux<PeersCommunicator> uploadFlux(TorrentStatus torrentStatus,
                                               TorrentFileSystemManager torrentFileSystemManager,
                                               Flux<PeersCommunicator> peersCommunicatorFlux) {
        return peersCommunicatorFlux.flatMap(peersCommunicator ->
                peersCommunicator.receivePeerMessages()
                        .getRequestMessageResponseFlux()
                        .flatMap(requestMessage -> torrentFileSystemManager.buildPieceMessage(requestMessage)
                                .onErrorResume(PieceNotDownloadedYetException.class, throwable -> Mono.empty()))
                        .flatMap(pieceMessage -> peersCommunicator.sendPieceMessage(pieceMessage.getIndex(),
                                pieceMessage.getBegin(), pieceMessage.getBlock())));
    }
}
