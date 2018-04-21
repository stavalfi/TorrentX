package main.algorithms.impls.v1.notification;

import main.TorrentInfo;
import main.algorithms.NotifyAboutCompletedPieceAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.TorrentFileSystemManager;
import main.peer.Link;
import main.peer.peerMessages.PieceMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NotifyAboutCompletedPieceAlgorithmImpl implements NotifyAboutCompletedPieceAlgorithm {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private TorrentFileSystemManager torrentFileSystemManager;
    private Flux<Link> recordedFreePeerFlux;

    private Flux<Integer> notifiedCompletedPiecesFlux;

    public NotifyAboutCompletedPieceAlgorithmImpl(TorrentInfo torrentInfo,
                                                  TorrentStatus torrentStatus,
                                                  TorrentFileSystemManager torrentFileSystemManager,
                                                  Flux<Link> recordedFreePeerFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.torrentFileSystemManager = torrentFileSystemManager;
        this.recordedFreePeerFlux = recordedFreePeerFlux;

        this.notifiedCompletedPiecesFlux =
                this.torrentFileSystemManager.savedBlockFlux()
                        .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                        .map(PieceEvent::getReceivedPiece)
                        .map(PieceMessage::getIndex)
                        .flatMap(completedPiece ->
                                this.recordedFreePeerFlux.map(Link::sendMessages)
                                        .flatMap(sendPeerMessages -> sendPeerMessages.sendHaveMessage(completedPiece))
                                        .timeout(Duration.ofSeconds(1))
                                        // I will never complete the following line because recordedFreePeerFlux
                                        // never ends so I stop listening to it when I don't get peer per sec from him.
                                        // then I will signal (only once) the index of the piece which was completed.
                                        .collectList().map(sendPeerMessagesList -> completedPiece)
                                        .onErrorResume(TimeoutException.class, throwable -> Mono.just(completedPiece))
                        )
                        .publish()
                        .autoConnect(0);
    }

    @Override
    public Flux<Integer> getNotifiedCompletedPiecesFlux() {
        return this.notifiedCompletedPiecesFlux;
    }
}
