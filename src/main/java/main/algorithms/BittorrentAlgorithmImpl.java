package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import main.torrent.status.TorrentStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private static Logger logger = LoggerFactory.getLogger(BittorrentAlgorithmImpl.class);

    private ConnectableFlux<TorrentPieceChanged> startUploadFlux;
    private ConnectableFlux<TorrentPieceChanged> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.startUploadFlux = uploadFlux(torrentInfo, torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish();
        this.startUploadFlux.connect();

        this.startDownloadFlux = downloadFlux(torrentInfo, torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish();
        this.startDownloadFlux.connect();
    }

    private Flux<TorrentPieceChanged> uploadFlux(TorrentInfo torrentInfo,
                                                 TorrentStatus torrentStatus,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> peersCommunicatorFlux) {
        return peersCommunicatorFlux.flatMap(peersCommunicator ->
                peersCommunicator.receivePeerMessages()
                        .getRequestMessageResponseFlux()
                        .filter(requestMessage -> torrentStatus.isUploading())
                        .flatMap(requestMessage -> torrentFileSystemManager.buildPieceMessage(requestMessage)
                                .onErrorResume(PieceNotDownloadedYetException.class, throwable -> Mono.empty()))
                        .flatMap(pieceMessage ->
                                peersCommunicator.sendMessages().sendPieceMessage(pieceMessage.getIndex(),
                                        pieceMessage.getBegin(), pieceMessage.getBlock())
                                        .map(__ -> new TorrentPieceChanged(pieceMessage.getIndex(),
                                                torrentInfo.getPieces().get(pieceMessage.getIndex()),
                                                TorrentPieceStatus.UPLOADING))));
    }

    private Flux<TorrentPieceChanged> downloadFlux(TorrentInfo torrentInfo,
                                                   TorrentStatus torrentStatus,
                                                   TorrentFileSystemManager torrentFileSystemManager,
                                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        ConnectableFlux<PeersCommunicator> recordedPeersFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendBitFieldMessage(torrentFileSystemManager
                                .buildBitFieldMessage(peersCommunicator.getMe(), peersCommunicator.getPeer()))
                                .flatMap(sendPeerMessages -> sendPeerMessages.sendInterestedMessage())
                                .map(sendPeerMessages -> peersCommunicator))
                .replay();

        // start recording connected peers
        recordedPeersFlux.connect();

        Flux<PeersCommunicator> informAllPeersAboutCompletedPiecesFlux = torrentFileSystemManager.savedBlockFlux()
                .doOnNext(torrentPieceChanged -> logger.info(torrentPieceChanged.toString()))
                .filter(torrentPieceChanged -> torrentPieceChanged.equals(TorrentPieceStatus.COMPLETED))
                .flatMap(torrentPieceChanged ->
                        recordedPeersFlux.flatMap(peersCommunicator ->
                                peersCommunicator.sendMessages()
                                        .sendHaveMessage(torrentPieceChanged.getPieceIndex())
                                        .map(sendPeerMessages -> peersCommunicator)));
        informAllPeersAboutCompletedPiecesFlux.subscribe();

        int minMissingPieceIndex = torrentFileSystemManager.minMissingPieceIndex();
        int maxMissingPieceIndex = torrentFileSystemManager.maxMissingPieceIndex();
        int piecesAmountToDownloadAtMost = maxMissingPieceIndex - minMissingPieceIndex + 1;

        // check if the download has completed already.
        if (minMissingPieceIndex == -1 || maxMissingPieceIndex == -1)
            return Flux.empty();

        Flux.range(minMissingPieceIndex, piecesAmountToDownloadAtMost)
                .filter(pieceIndex -> torrentFileSystemManager.havePiece(pieceIndex))
                .flatMap(pieceIndex ->
                        getPieceFlux(torrentInfo, torrentStatus, pieceIndex,
                                torrentFileSystemManager, recordedPeersFlux), 2);
        return Flux.empty();
    }

    private Flux<PeersCommunicator> getPieceFlux(TorrentInfo torrentInfo,
                                                 TorrentStatus torrentStatus,
                                                 int pieceIndex,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> recordedPeersFlux) {
        int pieceLength = torrentFileSystemManager.getTorrentInfo().getPieceLength(pieceIndex);
        int estimatedPieceStatus = torrentFileSystemManager.getPiecesEstimatedStatus()[pieceIndex];

        ConnectableFlux<Boolean> isInDownloadingStateFlux = torrentStatus.getStatusTypeFlux()
                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD) ||
                        torrentStatusType.equals(TorrentStatusType.PAUSE_DOWNLOAD))
                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD))
                .replay(1);
        isInDownloadingStateFlux.connect();

        return recordedPeersFlux.filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim())
                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus()
                                .getPiecesStatus()
                                .get(pieceIndex))
                .flatMap(peersCommunicator ->
                        isInDownloadingStateFlux.filter(isInDownloadingState -> isInDownloadingState)
                                .map(__ -> peersCommunicator))
                // I will be here when I'm in downloading state only.
                .flatMap(peersCommunicator ->
                        buildRequestMessagesToPiece(peersCommunicator.getMe(), peersCommunicator.getPeer(), pieceIndex, pieceLength, estimatedPieceStatus)
                                .flatMap(requestMessage -> peersCommunicator.sendMessages()
                                        .sendRequestMessage(requestMessage), 1)
                                // I don't want to emit the same peersCommunicator object multiple times.
                                .collectList()
                                .map(sentRequests -> peersCommunicator));
    }

    private Flux<RequestMessage> buildRequestMessagesToPiece(Peer from, Peer to, int pieceIndex,
                                                             int pieceLength, int pieceEstimatedStatus) {
        final int REQUEST_BLOCK_SIZE = 16_000;
        ArrayList<RequestMessage> requests = new ArrayList();
        for (int i = pieceEstimatedStatus; i < pieceLength; i += REQUEST_BLOCK_SIZE) {
            requests.add(new RequestMessage(from, to, pieceIndex, i, REQUEST_BLOCK_SIZE));
        }
        return Flux.fromIterable(requests);
    }

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    @Override
    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.startUploadFlux;
    }
}
