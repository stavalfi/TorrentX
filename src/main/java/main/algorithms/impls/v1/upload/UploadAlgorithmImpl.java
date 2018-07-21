package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private Flux<PieceEvent> uploadedBlocks$;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               Store<TorrentStatusState, TorrentStatusAction> store,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {

        Flux<TorrentStatusState> startUpload$ = store.statesByAction(TorrentStatusAction.START_UPLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_UPLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.uploadedBlocks$ = store.statesByAction(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.RESUME_UPLOAD_SELF_RESOLVED))
                .publishOn(Schedulers.newSingle("PEERS-RECEIVER-FOR-TORRENT-" + torrentInfo.getName()))
                .concatMap(__ -> peersCommunicatorFlux)
                .flatMap(link -> link.receivePeerMessages().getRequestMessageResponseFlux()
                        .filter(requestMessage -> fileSystemLink.havePiece(requestMessage.getIndex()))
                        .concatMap(requestMessage -> store.notifyWhen(TorrentStatusAction.RESUME_UPLOAD_WIND_UP, requestMessage))
                        .concatMap(requestMessage -> fileSystemLink.buildPieceMessage(requestMessage))
                        .concatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                                .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage))))
                .publish()
                .autoConnect(0);

        Flux<TorrentStatusState> pauseUpload$ = store.statesByAction(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_UPLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocks$() {
        return this.uploadedBlocks$;
    }
}
