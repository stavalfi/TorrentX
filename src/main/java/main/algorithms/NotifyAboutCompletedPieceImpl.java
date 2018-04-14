package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NotifyAboutCompletedPieceImpl {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<PeersCommunicator> recordedFreePeerFlux;

    private Flux<Integer> notifyAboutCompletedPieceFlux;

    public NotifyAboutCompletedPieceImpl(TorrentInfo torrentInfo,
                                         TorrentStatus torrentStatus,
                                         TorrentFileSystemManager torrentFileSystemManager,
                                         Flux<PeersCommunicator> recordedFreePeerFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.recordedFreePeerFlux = recordedFreePeerFlux;

        this.notifyAboutCompletedPieceFlux = notifyAboutCompletedPiecesFlux(torrentStatus)
                .publish()
                .autoConnect(0);
    }

    private Flux<Integer> notifyAboutCompletedPiecesFlux(TorrentStatus torrentStatus) {
        return torrentStatus.isStartedDownloadingFlux()
                .filter(isDownloadStarted -> isDownloadStarted)
                .flatMap(__ -> this.torrentFileSystemManager.savedBlockFlux())
                .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(TorrentPieceChanged::getPieceIndex)
                .flatMap(completedPiece ->
                        this.recordedFreePeerFlux.map(PeersCommunicator::sendMessages)
                                .flatMap(sendPeerMessages -> sendPeerMessages.sendHaveMessage(completedPiece))
                                .timeout(Duration.ofSeconds(1))
                                // I will never complete the following line because recordedFreePeerFlux
                                // never ends so I stop listening to it when I don't get peer per sec from him.
                                // then I will signal (only once) the index of the piece which was completed.
                                .collectList().map(sendPeerMessagesList -> completedPiece)
                                .onErrorResume(TimeoutException.class, throwable -> Mono.just(completedPiece))
                );
    }

    public Flux<Integer> getNotifyAboutCompletedPieceFlux() {
        return this.notifyAboutCompletedPieceFlux;
    }
}
