package main.statistics;

import main.peer.Peer;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class DefaultStatisticsReporter implements StatisticsReporter {
    @Override
    public Flux<Double> calculateDownloadPercentagesOfTorrentAsync(long fileSize, Flux<Integer> downloadedBytesFlux, Duration period) {
        return Flux.never();
    }

    @Override
    public Flux<Double> calculateUploadRateOfPeerAsync(Peer peer, Flux<PieceMessage> pieceMessageFlux, Duration period) {
        return Flux.never();
    }

    @Override
    public Flux<Double> calculateDownloadRateOfPeerAsync(Peer peer, Flux<PieceMessage> pieceMessageFlux, Duration period) {
        return Flux.never();
    }
}
