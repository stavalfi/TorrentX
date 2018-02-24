package main.statistics;

import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;

public interface StatisticsReporter {
    /**
     * Calculate the download percentage of this torrent every period of time.
     *
     * @param fileSize            in bytes.
     * @param downloadedBytesFlux amount of incoming bytes which was successfully downloaded by this application.
     * @param period              of time to wait before each evaluation.
     * @return flux of numbers which represents the download percentage of this torrent.
     */
    Flux<Double> calculateDownloadPercentagesOfTorrentAsync(long fileSize,
                                                            Flux<Integer> downloadedBytesFlux,
                                                            Duration period);

    /**
     * Calculate the upload rate of a peer every period of time.
     *
     * @param peer
     * @param pieceMessageFlux pieces the peer sent me.
     * @param period
     * @return flux of numbers which represents the upload rate of a peer.
     */
    Flux<Double> calculateUploadRateOfPeerAsync(Peer peer,
                                                Flux<PieceMessage> pieceMessageFlux,
                                                Duration period);

    /**
     * Calculate the download rate of a peer every period of time.
     *
     * @param peer
     * @param pieceMessageFlux pieces the peer received from me.
     * @param period
     * @return flux of numbers which represents the download rate of a peer.
     */
    Flux<Double> calculateDownloadRateOfPeerAsync(Peer peer,
                                                  Flux<PieceMessage> pieceMessageFlux,
                                                  Duration period);
}
