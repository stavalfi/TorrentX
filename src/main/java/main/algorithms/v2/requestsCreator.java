package main.algorithms.v2;

import main.TorrentInfo;
import main.file.system.TorrentFileSystemManager;
import main.peer.Peer;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;

public class requestsCreator {
    private TorrentInfo torrentInfo;
    private TorrentFileSystemManager torrentFileSystemManager;

    public requestsCreator(TorrentInfo torrentInfo,
                           TorrentFileSystemManager torrentFileSystemManager) {
        this.torrentInfo = torrentInfo;
        this.torrentFileSystemManager = torrentFileSystemManager;
    }

    public Flux<RequestMessage> createRequestFlux(Peer me, Peer peer, int pieceIndex) {
        final int REQUEST_BLOCK_SIZE = 16_384;
        int pieceLength = this.torrentInfo.getPieceLength(pieceIndex);

        return Flux.generate(sink -> {
            int bytesDownloadedUntilNow =
                    this.torrentFileSystemManager.getPiecesEstimatedStatus()[pieceIndex];
            if (bytesDownloadedUntilNow == pieceLength) {
                sink.complete();
            }

            RequestMessage requestMessage = new RequestMessage(me, peer,
                    pieceIndex, bytesDownloadedUntilNow, REQUEST_BLOCK_SIZE);

            sink.next(requestMessage);
        });
    }
}
