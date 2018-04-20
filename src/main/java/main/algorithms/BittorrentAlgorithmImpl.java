package main.algorithms;

import main.TorrentInfo;
import main.algorithms.v2.DownloadAlgorithmImplV3;
import main.algorithms.v2.LinkForPiecesProvider;
import main.algorithms.v2.requestsCreator;
import main.downloader.TorrentPieceChanged;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private NotifyAboutCompletedPieceImpl notifyAboutCompletedPiece;
    private UploadBittorrentAlgorithmImpl uploadBittorrentAlgorithm;
    private DownloadAlgorithmImplV3 downloadBittorrentAlgorithm;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<Link> peersCommunicatorFlux) {

        Flux<Link> recordedFreePeerFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect();

        this.notifyAboutCompletedPiece = new NotifyAboutCompletedPieceImpl(torrentInfo,
                torrentStatus,
                torrentFileSystemManager,
                recordedFreePeerFlux);

        this.uploadBittorrentAlgorithm = new UploadBittorrentAlgorithmImpl(torrentInfo,
                torrentStatus,
                torrentFileSystemManager,
                peersCommunicatorFlux);

        LinkForPiecesProvider organizer =
                new LinkForPiecesProvider(recordedFreePeerFlux,
                        torrentFileSystemManager.getUpdatedPiecesStatus());

        requestsCreator requestsCreator =
                new requestsCreator(torrentInfo, torrentFileSystemManager);

        this.downloadBittorrentAlgorithm = new DownloadAlgorithmImplV3(
                torrentInfo,
                torrentStatus,
                torrentFileSystemManager,
                organizer,
                requestsCreator);
    }

    @Override
    public Flux<Integer> startDownloadFlux() {
        return this.downloadBittorrentAlgorithm.startDownloadFlux();
    }

    @Override
    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.uploadBittorrentAlgorithm.startUploadingFlux();
    }

    @Override
    public Flux<Integer> getNotifyAboutCompletedPieceFlux() {
        return this.notifyAboutCompletedPiece.getNotifyAboutCompletedPieceFlux();
    }
}
