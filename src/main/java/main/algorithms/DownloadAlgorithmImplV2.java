package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;

public class DownloadAlgorithmImplV2 {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<PeersCommunicator> recordedFreePeerFlux;
    private Flux<TorrentPieceChanged> recordedReceivedBlockFlux;

    private Flux<Integer> startDownloadFlux;

    DownloadAlgorithmImplV2(TorrentInfo torrentInfo,
                            TorrentStatus torrentStatus,
                            TorrentFileSystemManager torrentFileSystemManager,
                            Flux<PeersCommunicator> recordedFreePeerFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;

        this.recordedFreePeerFlux = recordedFreePeerFlux
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getAmIDownloadingFromHim());

        // I'm recording this because I'm sending requests and
        // I need to assert that I received what I asked for.
        this.recordedReceivedBlockFlux = torrentFileSystemManager.savedBlockFlux()
                .replay()
                .autoConnect(0);

//        this.startDownloadFlux = downloadFlux()
//                .publish()
//                .autoConnect(0);
    }

//    private Flux<Integer> downloadFlux() {
////        this.torrentStatus.notifyWhenStartedDownloading()
////                .flatMapMany(__ -> this.recordedFreePeerFlux)
////                .flatMap(peersCommunicator ->
////                        peersCommunicator.getPeerCurrentStatus().
////                        , 1);
//    }
}
