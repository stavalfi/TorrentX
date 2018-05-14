package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.torrent.status.TorrentStatusStore;
import reactor.core.publisher.Flux;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private TorrentStatusStore torrentStatusStore;
    private FileSystemLink fileSystemLink;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<PieceEvent> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               TorrentStatusStore torrentStatusStore,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatusStore = torrentStatusStore;
        this.fileSystemLink = fileSystemLink;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.uploadedBlocksFlux = Flux.never();
        // TODO: uncomment
//                this.torrentStatusStore
//                .getState$()
//                .filter(Status::isStartedUpload)
//                .take(1)
//                .flatMap(__ -> this.peersCommunicatorFlux)
//                .flatMap(peersCommunicator ->
//                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
//                        // notifyWhenStartUploading() thread. Watch out.
//                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
//                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
//                                .flatMap(requestMessage -> this.torrentStatusStore.getState$()
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
