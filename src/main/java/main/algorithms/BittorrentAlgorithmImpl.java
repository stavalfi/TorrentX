package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.ReceivePeerMessages;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import main.torrent.status.TorrentStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.function.Predicate;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private static Logger logger = LoggerFactory.getLogger(BittorrentAlgorithmImpl.class);

    private ConnectableFlux<TorrentPieceChanged> startUploadFlux;
    private ConnectableFlux<RequestMessage> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.startUploadFlux = uploadFlux(torrentInfo, torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish();
        this.startUploadFlux.connect();

        this.startDownloadFlux = downloadFlux(torrentInfo, torrentStatus,
                torrentFileSystemManager, peersCommunicatorFlux.publish().autoConnect())
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

    private Flux<RequestMessage> downloadFlux(TorrentInfo torrentInfo,
                                              TorrentStatus torrentStatus,
                                              TorrentFileSystemManager torrentFileSystemManager,
                                              Flux<PeersCommunicator> peersCommunicatorFlux) {
        Flux<PeersCommunicator> informAllPeersAboutCompletedPiecesFlux = torrentFileSystemManager.savedBlockFlux()
                .filter(torrentPieceChanged -> torrentPieceChanged.equals(TorrentPieceStatus.COMPLETED))
                .flatMap(torrentPieceChanged ->
                        peersCommunicatorFlux.flatMap(peersCommunicator ->
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

//        final int AMOUNT_OF_CONCURRENT_CONNECTIONS = 3;
        final int AMOUNT_OF_CONCURRENT_PIECES_DOWNLOADS = 1;

        Flux<PeersCommunicator> recordedPeersFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect()
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim());

//        getIndexOfMissingPieceFlux(torrentFileSystemManager)


        return Flux.range(minMissingPieceIndex, piecesAmountToDownloadAtMost)
                .filter(pieceIndex -> !torrentFileSystemManager.havePiece(pieceIndex))
                .flatMap(pieceIndex ->
                        getPieceFlux(torrentInfo, torrentStatus, pieceIndex,
                                torrentFileSystemManager, recordedPeersFlux), AMOUNT_OF_CONCURRENT_PIECES_DOWNLOADS);
    }

    private Flux<Integer> getIndexOfMissingPieceFlux(TorrentFileSystemManager torrentFileSystemManager) {
        int minMissingPieceIndex = torrentFileSystemManager.minMissingPieceIndex();
        int maxMissingPieceIndex = torrentFileSystemManager.maxMissingPieceIndex();
        int piecesAmountToDownloadAtMost = maxMissingPieceIndex - minMissingPieceIndex + 1;

        return Flux.range(minMissingPieceIndex, piecesAmountToDownloadAtMost)
                .filter(pieceIndex -> !torrentFileSystemManager.havePiece(pieceIndex));
    }

    private Flux<RequestMessage> getPieceFlux(TorrentInfo torrentInfo,
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

        Flux<Boolean> waitUntilWeInDownloadState =
                isInDownloadingStateFlux.filter(isInDownloadingState -> isInDownloadingState);

        Predicate<PeersCommunicator> isPeerHavePiece = peersCommunicator ->
                peersCommunicator.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex);

        final int AMOUNT_OF_CONCURRENT_REQUESTS = 3;

        return recordedPeersFlux.filter(isPeerHavePiece)
                .flatMap(peersCommunicator -> waitUntilWeInDownloadState
                        .flatMap(__ -> buildRequestMessagesToPiece(peersCommunicator.getMe(),
                                peersCommunicator.getPeer(), pieceIndex, pieceLength, estimatedPieceStatus)
                                        .flatMap(requestMessage -> getPieceFromPeer(peersCommunicator, requestMessage))
                                , AMOUNT_OF_CONCURRENT_REQUESTS));
    }

    private Flux<RequestMessage> buildRequestMessagesToPiece(Peer from, Peer to, int pieceIndex,
                                                             int pieceLength, int pieceEstimatedStatus) {
        final int REQUEST_BLOCK_SIZE = 16_384;
        ArrayList<RequestMessage> requests = new ArrayList();
        for (int i = pieceEstimatedStatus; i < pieceLength; i += REQUEST_BLOCK_SIZE) {
            requests.add(new RequestMessage(from, to, pieceIndex, i, REQUEST_BLOCK_SIZE));
        }
        return Flux.fromIterable(requests);
    }

    private Mono<RequestMessage> getPieceFromPeer(PeersCommunicator peersCommunicator,
                                                  RequestMessage requestMessage) {
        Predicate<PieceMessage> isPieceRelatedToRequest = pieceMessage ->
                pieceMessage.getIndex() == requestMessage.getIndex() &&
                        pieceMessage.getBegin() == requestMessage.getBegin();

        return peersCommunicator.sendMessages()
                .sendRequestMessage(requestMessage)
                .map(sendPeerMessages -> peersCommunicator.receivePeerMessages())
                .flatMapMany(ReceivePeerMessages::getPieceMessageResponseFlux)
                .filter(isPieceRelatedToRequest)
                .single()
                .map(pieceMessage -> requestMessage);
    }

    @Override
    public Flux<RequestMessage> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    @Override
    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.startUploadFlux;
    }
}
