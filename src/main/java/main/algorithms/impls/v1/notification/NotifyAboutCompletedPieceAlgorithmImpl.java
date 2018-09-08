package main.algorithms.impls.v1.notification;

import main.TorrentInfo;
import main.algorithms.NotifyAboutCompletedPieceAlgorithm;
import main.algorithms.PieceEvent;
import main.algorithms.impls.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.peerMessages.PieceMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NotifyAboutCompletedPieceAlgorithmImpl implements NotifyAboutCompletedPieceAlgorithm {
    private TorrentInfo torrentInfo;
    private Store<TorrentStatusState, TorrentStatusAction> store;
    private FileSystemLink fileSystemLink;
    private Flux<Link> recordedFreePeer$;

    private Flux<Integer> notifiedCompletedPiecesFlux;

    public NotifyAboutCompletedPieceAlgorithmImpl(TorrentInfo torrentInfo,
                                                  Store<TorrentStatusState, TorrentStatusAction> store,
                                                  FileSystemLink fileSystemLink,
                                                  Flux<Link> recordedFreePeer$) {
        this.torrentInfo = torrentInfo;
        this.store = store;
        this.fileSystemLink = fileSystemLink;
        this.recordedFreePeer$ = recordedFreePeer$;

        this.notifiedCompletedPiecesFlux =
                this.fileSystemLink.savedBlocks$()
                        .filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                        .map(PieceEvent::getReceivedPiece)
                        .map(PieceMessage::getIndex)
                        .buffer(5)
                        .flatMap(Flux::fromIterable)
                        .flatMap(completedPiece ->
                                this.recordedFreePeer$.map(Link::sendMessages)
                                        .flatMap(sendPeerMessages -> sendPeerMessages.sendHaveMessage(completedPiece))
                                        .timeout(Duration.ofSeconds(1),Schedulers.single())
                                        // I will never complete the following line because recordedFreePeer$
                                        // never ends so I stop listening to it when I don't get peer per sec from him.
                                        // then I will signal (only once) the index of the piece which was completed.
                                        .collectList().map(sendPeerMessagesList -> completedPiece)
                                        .onErrorResume(TimeoutException.class, throwable -> Mono.just(completedPiece)))
                        .publish()
                        .autoConnect(1);
    }

    @Override
    public Flux<Integer> getNotifiedCompletedPiecesFlux() {
        return this.notifiedCompletedPiecesFlux;
    }
}
