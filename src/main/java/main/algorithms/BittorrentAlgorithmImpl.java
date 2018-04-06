package main.algorithms;

import main.downloader.TorrentPieceChanged;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.peer.ReceivedMessagesImpl;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private Flux<TorrentPieceChanged> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.startDownloadFlux = downloadFlux(torrentStatus,
                torrentFileSystemManager, peersCommunicatorFlux)
                .publish();

        ((ConnectableFlux<TorrentPieceChanged>) this.startDownloadFlux).connect();

        uploadFlux(torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish()
                .connect();
    }

    private Flux<TorrentPieceChanged> downloadFlux(TorrentStatus torrentStatus,
                                                   TorrentFileSystemManager torrentFileSystemManager,
                                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        ReceivedMessagesImpl receiveTorrentMessagesMessagesFlux = new ReceivedMessagesImpl(
                peersCommunicatorFlux.map(PeersCommunicator::receivePeerMessages));

        return Flux.empty();
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

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }
}
