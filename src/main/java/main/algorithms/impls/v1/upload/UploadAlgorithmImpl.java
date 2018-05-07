package main.algorithms.impls.v1.upload;

import main.TorrentInfo;
import main.algorithms.UploadAlgorithm;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.file.system.AllocatedBlock;
import main.file.system.BlocksAllocatorImpl;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.peerMessages.PieceMessage;
import main.torrent.status.Status;
import main.torrent.status.StatusChanger;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;

public class UploadAlgorithmImpl implements UploadAlgorithm {
    private TorrentInfo torrentInfo;
    private StatusChanger statusChanger;
    private FileSystemLink fileSystemLink;
    private Flux<Link> peersCommunicatorFlux;

    private Flux<PieceEvent> uploadedBlocksFlux;

    public UploadAlgorithmImpl(TorrentInfo torrentInfo,
                               StatusChanger statusChanger,
                               FileSystemLink fileSystemLink,
                               Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.statusChanger = statusChanger;
        this.fileSystemLink = fileSystemLink;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        BiFunction<PieceEvent, AllocatedBlock, PieceEvent> updateAllocatedBlock = (pieceEvent, allocatedBlock) ->
                new PieceEvent(pieceEvent.getTorrentPieceStatus(),
                        new PieceMessage(pieceEvent.getReceivedPiece().getFrom(),
                                pieceEvent.getReceivedPiece().getTo(),
                                pieceEvent.getReceivedPiece().getIndex(),
                                pieceEvent.getReceivedPiece().getBegin(),
                                allocatedBlock,
                                torrentInfo.getPieceLength(pieceEvent.getReceivedPiece().getIndex())));

        this.uploadedBlocksFlux = this.statusChanger
                .getStatus$()
                .filter(Status::isStartedUpload)
                .take(1)
                .flatMap(__ -> this.peersCommunicatorFlux)
                .flatMap(peersCommunicator ->
                        // if getRequestMessageResponseFlux is not running on unique thread, then I will block
                        // notifyWhenStartUploading() thread. Watch out.
                        peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux()
                                .filter(requestMessage -> this.fileSystemLink.havePiece(requestMessage.getIndex()))
                                .flatMap(requestMessage -> this.statusChanger.getStatus$()
                                        .filter(Status::isUploading)
                                        .take(1)
                                        .flatMap(__ -> BlocksAllocatorImpl.getInstance().allocate(0, requestMessage.getBlockLength()))
                                        .flatMap(allocatedBlock ->
                                                this.fileSystemLink.buildPieceMessage(requestMessage, allocatedBlock)
                                                        .flatMap(pieceMessage ->
                                                                peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                                                        pieceMessage.getBegin(), pieceMessage.getAllocatedBlock())
                                                                        .map(___ -> new PieceEvent(TorrentPieceStatus.UPLOADING, pieceMessage)))
                                                        .doOnNext(pieceEvent -> BlocksAllocatorImpl.getInstance().free(allocatedBlock))
                                                        .doOnError(throwable -> BlocksAllocatorImpl.getInstance().free(allocatedBlock))
                                                        .doOnCancel(() -> BlocksAllocatorImpl.getInstance().free(allocatedBlock)))))
                .publish()
                .autoConnect(0);
    }

    @Override
    public Flux<PieceEvent> getUploadedBlocksFlux() {
        return this.uploadedBlocksFlux;
    }
}
