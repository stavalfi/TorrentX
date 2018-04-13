package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeersCommunicator;
import main.peer.SendPeerMessages;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private Flux<TorrentPieceChanged> startUploadFlux;
    private Flux<RequestMessage> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.startUploadFlux = torrentStatus.isStartedUploadingFlux()
                .filter(isUploadingStarted -> isUploadingStarted)
                .flatMap(__ ->
                        uploadFlux(torrentInfo, torrentStatus, torrentFileSystemManager, peersCommunicatorFlux))
                .publish()
                .autoConnect(0);

//        this.startDownloadFlux = downloadFlux(torrentInfo, torrentStatus,
//                torrentFileSystemManager, peersCommunicatorFlux.publish().autoConnect())
//                .publish();
        this.startDownloadFlux = Flux.never();
        // this.startDownloadFlux.connect();
    }

    private Flux<TorrentPieceChanged> uploadFlux(TorrentInfo torrentInfo,
                                                 TorrentStatus torrentStatus,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> peersCommunicatorFlux) {
        return peersCommunicatorFlux.flatMap(peersCommunicator ->
                peersCommunicator.receivePeerMessages()
                        .getRequestMessageResponseFlux()
                        .flatMap(requestMessage -> torrentStatus.isUploadingFlux()
                                .filter(isUploading -> isUploading)
                                .flatMap(__ -> torrentFileSystemManager.buildPieceMessage(requestMessage)
                                        .onErrorResume(PieceNotDownloadedYetException.class, throwable -> Mono.empty())))
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

        Flux<PeersCommunicator> recordedPeersFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendMessages().sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                .replay()
                .autoConnect()
                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim());

        recordedPeersFlux.flatMap(peersCommunicator ->
                sendSingleRequestMessageForMissingPiece(torrentFileSystemManager, peersCommunicator))
                .take(1)
                .subscribe(__ -> System.out.println("downloaded something!!"),
                        Throwable::printStackTrace,
                        () -> System.out.println("sent the first request to a random piece"));

        return Flux.empty();
    }

    // download one random block to trigger the torrentFileSystemManager.savedBlockFlux().
    private Mono<SendPeerMessages> sendSingleRequestMessageForMissingPiece(TorrentFileSystemManager torrentFileSystemManager,
                                                                           PeersCommunicator peersCommunicator) {
        int minMissingPieceIndex = torrentFileSystemManager.minMissingPieceIndex();

        // check if the download has completed already.
        if (minMissingPieceIndex == -1)
            return Mono.empty();

        final int REQUEST_BLOCK_SIZE = 16_384;
        RequestMessage requestMessage = new RequestMessage(peersCommunicator.getMe(),
                peersCommunicator.getPeer(),
                minMissingPieceIndex,
                torrentFileSystemManager.getPiecesEstimatedStatus()[minMissingPieceIndex],
                REQUEST_BLOCK_SIZE);

        // there is no way i'm downloading for him now because I just started download this torrent.
        // so currently, this is only for testing.
        return peersCommunicator.getPeerCurrentStatus()
                .getAmIDownloadingFromHimFlux()
                .filter(amIDownloadingForHim -> !amIDownloadingForHim)
                .flatMap(__ -> peersCommunicator.sendMessages()
                        .sendRequestMessage(requestMessage))
                .take(1)
                .single();
    }


//    private Flux<RequestMessage> downloadFlux(TorrentInfo torrentInfo,
//                                              TorrentStatus torrentStatus,
//                                              TorrentFileSystemManager torrentFileSystemManager,
//                                              Flux<PeersCommunicator> peersCommunicatorFlux) {
////        Flux<PeersCommunicator> informAllPeersAboutCompletedPiecesFlux = torrentFileSystemManager.savedBlockFlux()
////                .filter(torrentPieceChanged -> torrentPieceChanged.equals(TorrentPieceStatus.COMPLETED))
////                .flatMap(torrentPieceChanged ->
////                        peersCommunicatorFlux.flatMap(peersCommunicator ->
////                                peersCommunicator.sendMessages()
////                                        .sendHaveMessage(torrentPieceChanged.getPieceIndex())
////                                        .map(sendPeerMessages -> peersCommunicator)));
////        informAllPeersAboutCompletedPiecesFlux.subscribe();
//
////        int minMissingPieceIndex = torrentFileSystemManager.minMissingPieceIndex();
////        int maxMissingPieceIndex = torrentFileSystemManager.maxMissingPieceIndex();
////        int piecesAmountToDownloadAtMost = maxMissingPieceIndex - minMissingPieceIndex + 1;
////
////        // check if the download has completed already.
////        if (minMissingPieceIndex == -1 || maxMissingPieceIndex == -1)
////            return Flux.empty();
////
////        final int AMOUNT_OF_CONCURRENT_CONNECTIONS = 3;
////        final int AMOUNT_OF_CONCURRENT_PIECES_DOWNLOADS = 1;
//
//        Flux<PeersCommunicator> recordedPeersFlux = peersCommunicatorFlux
//                .flatMap(peersCommunicator ->
//                        peersCommunicator.sendMessages().sendInterestedMessage()
//                                .map(sendPeerMessages -> peersCommunicator))
//                .replay()
//                .autoConnect()
//                .filter(peersCommunicator -> !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
//                .filter(peersCommunicator -> peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim());
//
////        ConnectableFlux<Boolean> isInDownloadingStateFlux = torrentStatus.getStatusTypeFlux()
////                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD) ||
////                        torrentStatusType.equals(TorrentStatusType.PAUSE_DOWNLOAD))
////                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD))
////                .replay(1);
////        isInDownloadingStateFlux.connect();
//
//        recordedPeersFlux.flatMap(peersCommunicator ->
//                sendSingleRequestMessageForMissingPiece(torrentFileSystemManager, peersCommunicator))
//                .subscribe(System.out::println,
//                        Throwable::printStackTrace,
//                        () -> System.out.println("sent the first request to a random piece"));
//
//
////        return Flux.range(minMissingPieceIndex, piecesAmountToDownloadAtMost)
////                .filter(pieceIndex -> !torrentFileSystemManager.havePiece(pieceIndex))
////                .flatMap(pieceIndex ->
////                        getPieceFlux(torrentInfo, torrentStatus, pieceIndex,
////                                torrentFileSystemManager, recordedPeersFlux), AMOUNT_OF_CONCURRENT_PIECES_DOWNLOADS);
//    }

//    private Flux<RequestMessage> buildRequestMessagesToPiece(Peer from, Peer to, int pieceIndex,
//                                                             int pieceLength, int pieceEstimatedStatus) {
//        final int REQUEST_BLOCK_SIZE = 16_384;
//        ArrayList<RequestMessage> requests = new ArrayList();
//        for (int i = pieceEstimatedStatus; i < pieceLength; i += REQUEST_BLOCK_SIZE) {
//            requests.add(new RequestMessage(from, to, pieceIndex, i, REQUEST_BLOCK_SIZE));
//        }
//        return Flux.fromIterable(requests);
//    }

//    private Mono<RequestMessage> getPieceFromPeer(PeersCommunicator peersCommunicator,
//                                                  RequestMessage requestMessage) {
//        Predicate<PieceMessage> isPieceRelatedToRequest = pieceMessage ->
//                pieceMessage.getIndex() == requestMessage.getIndex() &&
//                        pieceMessage.getBegin() == requestMessage.getBegin();
//
//        return peersCommunicator.sendMessages()
//                .sendRequestMessage(requestMessage)
//                .map(sendPeerMessages -> peersCommunicator.receivePeerMessages())
//                .flatMapMany(ReceivePeerMessages::getPieceMessageResponseFlux)
//                .filter(isPieceRelatedToRequest)
//                .single()
//                .map(pieceMessage -> requestMessage);
//    }
//
//    private Flux<RequestMessage> getPieceFlux(TorrentInfo torrentInfo,
//                                              TorrentStatus torrentStatus,
//                                              int pieceIndex,
//                                              TorrentFileSystemManager torrentFileSystemManager,
//                                              Flux<PeersCommunicator> recordedPeersFlux) {
//        int pieceLength = torrentFileSystemManager.getTorrentInfo().getPieceLength(pieceIndex);
//        int estimatedPieceStatus = torrentFileSystemManager.getPiecesEstimatedStatus()[pieceIndex];
//
//        ConnectableFlux<Boolean> isInDownloadingStateFlux = torrentStatus.getStatusTypeFlux()
//                .filter(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD) ||
//                        torrentStatusType.equals(TorrentStatusType.PAUSE_DOWNLOAD))
//                .map(torrentStatusType -> torrentStatusType.equals(TorrentStatusType.RESUME_DOWNLOAD))
//                .replay(1);
//        isInDownloadingStateFlux.connect();
//
//        Flux<Boolean> waitUntilWeInDownloadState =
//                isInDownloadingStateFlux.filter(isInDownloadingState -> isInDownloadingState);
//
//        Predicate<PeersCommunicator> isPeerHavePiece = peersCommunicator ->
//                peersCommunicator.getPeerCurrentStatus().getPiecesStatus().get(pieceIndex);
//
//        final int AMOUNT_OF_CONCURRENT_REQUESTS = 3;
//
//        return recordedPeersFlux.filter(isPeerHavePiece)
//                .flatMap(peersCommunicator -> waitUntilWeInDownloadState
//                        .flatMap(__ -> buildRequestMessagesToPiece(peersCommunicator.getMe(),
//                                peersCommunicator.getPeer(), pieceIndex, pieceLength, estimatedPieceStatus)
//                                        .flatMap(requestMessage -> getPieceFromPeer(peersCommunicator, requestMessage))
//                                , AMOUNT_OF_CONCURRENT_REQUESTS));
//    }

    @Override
    public Flux<RequestMessage> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    @Override
    public Flux<TorrentPieceChanged> startUploadingFlux() {
        return this.startUploadFlux;
    }
}
