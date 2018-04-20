package main.algorithms.impls;

import main.TorrentInfo;
import main.algorithms.*;
import main.algorithms.impls.v1.download.*;
import main.algorithms.impls.v1.notification.NotifyAboutCompletedPieceAlgorithmImpl;
import main.algorithms.impls.v1.upload.UploadAlgorithmImpl;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmInitializer {
    public static BittorrentAlgorithm v1(TorrentInfo torrentInfo,
                                         TorrentStatus torrentStatus,
                                         TorrentFileSystemManager torrentFileSystemManager,
                                         Flux<Link> peersCommunicatorFlux) {
        Flux<Link> recordedPeerFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect();

        NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm =
                new NotifyAboutCompletedPieceAlgorithmImpl(torrentInfo,
                        torrentStatus,
                        torrentFileSystemManager,
                        recordedPeerFlux);

        UploadAlgorithm uploadAlgorithm = new UploadAlgorithmImpl(torrentInfo,
                torrentStatus,
                torrentFileSystemManager,
                peersCommunicatorFlux);

        PeersToPiecesMapper peersToPiecesMapper =
                new PeersToPiecesMapperImpl(recordedPeerFlux,
                        torrentFileSystemManager.getUpdatedPiecesStatus());

        BlockDownloader blockDownloader = new BlockDownloaderImpl(torrentInfo, torrentFileSystemManager);

        PiecesDownloader piecesDownloader = new PiecesDownloaderImpl(torrentInfo, torrentStatus,
                torrentFileSystemManager, peersToPiecesMapper, blockDownloader);

        DownloadAlgorithm downloadAlgorithm = new DownloadAlgorithm(piecesDownloader, blockDownloader, peersToPiecesMapper);

        return new BittorrentAlgorithm(uploadAlgorithm, downloadAlgorithm, notifyAboutCompletedPieceAlgorithm);
    }
}
