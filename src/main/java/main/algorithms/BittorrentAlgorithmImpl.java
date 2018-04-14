package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private NotifyAboutCompletedPieceImpl notifyAboutCompletedPiece;
    private UploadBittorrentAlgorithmImpl uploadBittorrentAlgorithm;
    private DownloadBittorrentAlgorithmImpl downloadBittorrentAlgorithm;

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

        this.downloadBittorrentAlgorithm = new DownloadBittorrentAlgorithmImpl(torrentInfo,
                torrentStatus,
                torrentFileSystemManager,
                recordedFreePeerFlux);
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
