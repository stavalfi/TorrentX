package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private Store<TorrentStatusState, TorrentStatusAction> store;
    private FileSystemLink fileSystemLink;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<PieceEvent> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               Store<TorrentStatusState, TorrentStatusAction> store,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.store = store;
        this.fileSystemLink = fileSystemLink;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.uploadedBlocksFlux = Flux.never();
        // TODO: uncomment
//                this.store
//                .getState$()
//                .filter(Status::isStartedUpload)
//                .take(1)
//                .flatMap(__ -> this.peersCommunicatorFlux)
//                .flatMap(peersCommunicator ->
//                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
//                        // notifyWhenStartUploading() thread. Watch out.
//                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
//                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
//                                .flatMap(requestMessage -> this.store.getState$()
//                                        .filter(Status::isUploading)
//                                        .take(1)
//                                        .flatMap(PieceMessage -> this.fileSystemLink.buildPieceMessage(requestMessage))
//                                        .flatMap(pieceMessage -> peersCommunicator.sendMessages().sendPieceMessage(pieceMessage)
//                                                .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage)))))
//                .publish()
//                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
