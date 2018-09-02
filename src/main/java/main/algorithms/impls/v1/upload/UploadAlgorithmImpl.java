package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

import java.util.AbstractMap;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private static Logger logger = LoggerFactory.getLogger(UploadAlgorithmImpl.class);
    private static Scheduler uploaderScheduler = Schedulers.newSingle("UPLOADER");
    private static Scheduler peersReceiverScheduler = Schedulers.newSingle("PEERS-RECEIVER-FOR-TORRENT");

    private Flux<PieceEvent> uploadedBlocks$;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               Store<TorrentStatusState, TorrentStatusAction> store,
                               FileSystemLink fileSystemLink,
                               Flux<AbstractMap.SimpleEntry<Link, RequestMessage>> incomingRequestMessages$) {

        Flux<TorrentStatusState> startUpload$ = store.statesByAction(TorrentStatusAction.START_UPLOAD_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.START_UPLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.uploadedBlocks$ = store.statesByAction(TorrentStatusAction.RESUME_UPLOAD_IN_PROGRESS)
                .publishOn(peersReceiverScheduler)
                /*
                Caution: There maybe a race condition when I miss signals of new requests (in tests when I fake incoming requests)
                because I only subscribe to them when I finish the following method.
                the solution is to report that I'm ready to upload only after I subscribed to the requests stream.
                */
                .concatMap(__ -> {
                    Flux<PieceEvent> uploader$ = incomingRequestMessages$.flatMap(tuple2 -> {
                        Link link = tuple2.getKey();
                        return Mono.just(tuple2.getValue())
                                .doOnNext(requestMessage -> logger.debug("App uploader - start analyze request: " + requestMessage))
                                .doOnNext(requestMessage -> logger.debug("App uploader - do I have the piece: " + requestMessage + " => " +
                                        fileSystemLink.havePiece(requestMessage.getIndex())))
                                .filter(requestMessage -> fileSystemLink.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> store.notifyWhen(TorrentStatusAction.RESUME_UPLOAD_WIND_UP, requestMessage))
                                .doOnNext(requestMessage -> logger.debug("start creating piece-message for response to peer because he sent me request-message: " + requestMessage))
                                .flatMap(fileSystemLink::buildPieceMessage)
                                .doOnNext(requestMessage -> logger.debug("end creating piece-message for response to peer because he sent me request-message: " + requestMessage))
                                .publishOn(UploadAlgorithmImpl.uploaderScheduler)
                                .flatMap(pieceMessage -> link.sendMessages().sendPieceMessage(pieceMessage)
                                        .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage)));
                    })
                            .replay()
                            .autoConnect(0);

                    store.dispatchNonBlocking(TorrentStatusAction.RESUME_UPLOAD_SELF_RESOLVED);

                    return uploader$;
                })
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
