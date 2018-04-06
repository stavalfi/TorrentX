package main.algorithms;

import main.downloader.TorrentPieceChanged;
import main.downloader.TorrentPieceStatus;
import main.file.system.PieceNotDownloadedYetException;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private static Logger logger = LoggerFactory.getLogger(BittorrentAlgorithmImpl.class);

    private Flux<TorrentPieceChanged> startDownloadFlux = Flux.empty();

    public BittorrentAlgorithmImpl(TorrentStatus torrentStatus,
                                   TorrentFileSystemManager torrentFileSystemManager,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        downloadFlux(torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish()
                .connect();

        uploadFlux(torrentStatus, torrentFileSystemManager, peersCommunicatorFlux)
                .publish()
                .connect();
    }

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    private Flux<PeersCommunicator> uploadFlux(TorrentStatus torrentStatus,
                                               TorrentFileSystemManager torrentFileSystemManager,
                                               Flux<PeersCommunicator> peersCommunicatorFlux) {
        return peersCommunicatorFlux.flatMap(peersCommunicator ->
                peersCommunicator.receivePeerMessages()
                        .getRequestMessageResponseFlux()
                        .flatMap(requestMessage -> torrentFileSystemManager.buildPieceMessage(requestMessage)
                                .onErrorResume(PieceNotDownloadedYetException.class, throwable -> Mono.empty()))
                        .flatMap(pieceMessage -> peersCommunicator.sendPieceMessage(pieceMessage.getIndex(),
                                pieceMessage.getBegin(), pieceMessage.getBlock())));
    }

    private Flux<TorrentPieceChanged> downloadFlux(TorrentStatus torrentStatus,
                                                   TorrentFileSystemManager torrentFileSystemManager,
                                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        ConnectableFlux<PeersCommunicator> recordedPeersFlux = peersCommunicatorFlux
                .flatMap(peersCommunicator ->
                        peersCommunicator.sendBitFieldMessage(torrentFileSystemManager
                                .buildBitFieldMessage(peersCommunicator.getMe(), peersCommunicator.getPeer())))
                .flatMap(peersCommunicator -> peersCommunicator.sendInterestedMessage())
                .replay();

        // start recording connected peers
        recordedPeersFlux.connect();

        Flux<PeersCommunicator> informAllPeersAboutCompletedPiecesFlux = torrentFileSystemManager.savedPiecesFlux()
                .doOnNext(torrentPieceChanged -> logger.info(torrentPieceChanged.toString()))
                .filter(torrentPieceChanged -> torrentPieceChanged.equals(TorrentPieceStatus.COMPLETED))
                .flatMap(torrentPieceChanged ->
                        recordedPeersFlux.flatMap(peersCommunicator ->
                                peersCommunicator.sendHaveMessage(torrentPieceChanged.getPieceIndex())));
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
                        getPieceFlux(pieceIndex, torrentFileSystemManager, recordedPeersFlux), 2);
        return Flux.empty();
    }

    private Flux<PeersCommunicator> getPieceFlux(int pieceIndex,
                                                 TorrentFileSystemManager torrentFileSystemManager,
                                                 Flux<PeersCommunicator> recordedPeersFlux) {
        int pieceLength = torrentFileSystemManager.getTorrentInfo().getPieceLength(pieceIndex);
        int estimatedPieceStatus = torrentFileSystemManager.getPiecesEstimatedStatus()[pieceIndex];
        return recordedPeersFlux.filter(peersCommunicator ->
                !peersCommunicator.getPeerCurrentStatus().getIsHeChokingMe())
                .filter(peersCommunicator ->
                        peersCommunicator.getPeerCurrentStatus().getAmIInterestedInHim())
                .filter(peersCommunicator ->
                        peersCommunicator.getPeerCurrentStatus()
                                .getPiecesStatus()
                                .get(pieceIndex))
                .flatMap(peersCommunicator ->
                        Flux.fromIterable(buildRequestMessagesToPiece(peersCommunicator.getMe(), peersCommunicator.getPeer(), pieceIndex, pieceLength, estimatedPieceStatus))
                                .flatMap(peersCommunicator::sendRequestMessage, 1)
                                // I don't want to emit the same peersCommunicator object multiple times.
                                .collectList()
                                .map(sentRequests -> peersCommunicator));
    }

    private List<RequestMessage> buildRequestMessagesToPiece(Peer from, Peer to, int pieceIndex,
                                                             int pieceLength, int pieceEstimatedStatus) {
        final int REQUEST_BLOCK_SIZE = 16_000;
        ArrayList<RequestMessage> requests = new ArrayList();
        for (int i = pieceEstimatedStatus; i < pieceLength; i += REQUEST_BLOCK_SIZE) {
            requests.add(new RequestMessage(from, to, pieceIndex, i, REQUEST_BLOCK_SIZE));
        }
        return requests;
    }

}