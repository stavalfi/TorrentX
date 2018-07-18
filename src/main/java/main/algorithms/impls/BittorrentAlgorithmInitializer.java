package main.algorithms.impls;

import main.TorrentInfo;
import main.algorithms.*;
import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.algorithms.impls.v1.download.PeersToPiecesMapperImpl;
import main.algorithms.impls.v1.download.PiecesDownloaderImpl;
import main.algorithms.impls.v1.notification.NotifyAboutCompletedPieceAlgorithmImpl;
import main.algorithms.impls.v1.upload.UploadAlgorithmImpl;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.peer.Link;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class BittorrentAlgorithmInitializer {
    public static BittorrentAlgorithm v1(AllocatorStore allocatorStore,
                                         TorrentInfo torrentInfo,
                                         Store<TorrentStatusState, TorrentStatusAction> store,
                                         FileSystemLink fileSystemLink,
                                         Flux<Link> peersCommunicatorFlux) {
        Flux<Link> recordedPeerFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect();

        NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm =
                new NotifyAboutCompletedPieceAlgorithmImpl(torrentInfo,
                        store,
                        fileSystemLink,
                        recordedPeerFlux);

        UploadAlgorithm uploadAlgorithm = new UploadAlgorithmImpl(torrentInfo,
                store,
                fileSystemLink,
                peersCommunicatorFlux);

        PeersToPiecesMapper peersToPiecesMapper =
                new PeersToPiecesMapperImpl(recordedPeerFlux,
                        fileSystemLink.getUpdatedPiecesStatus());

        BlockDownloader blockDownloader = new BlockDownloaderImpl(torrentInfo, fileSystemLink);

        PiecesDownloader piecesDownloader = new PiecesDownloaderImpl(allocatorStore,
                torrentInfo, store,
                fileSystemLink, peersToPiecesMapper, blockDownloader);

        DownloadAlgorithm downloadAlgorithm = new DownloadAlgorithm(piecesDownloader, blockDownloader, peersToPiecesMapper);

        return new BittorrentAlgorithm(uploadAlgorithm, downloadAlgorithm, notifyAboutCompletedPieceAlgorithm);
    }
}
