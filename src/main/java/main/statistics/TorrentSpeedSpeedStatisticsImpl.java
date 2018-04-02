package main.statistics;

import main.App;
import main.TorrentInfo;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;

public class TorrentSpeedSpeedStatisticsImpl implements SpeedStatistics {

    // don't change this number unless you change the expected results in the tests also.
    private final long rateInMillSeconds = 100;
    private TorrentInfo torrentInfo;

    private Flux<Double> downloadSpeedFlux;
    private Flux<Double> uploadSpeedFlux;


    public TorrentSpeedSpeedStatisticsImpl(TorrentInfo torrentInfo,
                                           Flux<? extends PeerMessage> receivedMessageMessages,
                                           Flux<? extends PeerMessage> sentSentMessages) {

        this.torrentInfo = torrentInfo;

        Flux<Double> intervalFlux =
                Flux.interval(Duration.ofMillis(this.rateInMillSeconds))
                        .map(interval -> 0)
                        .map(Double::new);

        Function<Flux<? extends PeerMessage>, Flux<Double>> pieceMessageToSize =
                messageFlux -> messageFlux
                        .filter(peerMessage -> peerMessage instanceof PieceMessage)
                        .cast(PieceMessage.class)
                        .map(PieceMessage::getBlock)
                        .map(array -> array.length)
                        .map(Double::new);

        Function<Flux<? extends PeerMessage>, Flux<Double>> messagesToSpeedFlux =
                pieceMessageToSize.andThen(pieceMessageSizeFlux ->
                        Flux.merge(pieceMessageSizeFlux, intervalFlux)
                                .buffer(Duration.ofMillis(this.rateInMillSeconds), App.MyScheduler)
                                .map(List::stream)
                                .map(doubleStream -> doubleStream.mapToDouble(Double::doubleValue))
                                .map(DoubleStream::sum));

        this.downloadSpeedFlux = messagesToSpeedFlux.apply(receivedMessageMessages);
        this.uploadSpeedFlux = messagesToSpeedFlux.apply(sentSentMessages);
    }

    public TorrentSpeedSpeedStatisticsImpl(TorrentInfo torrentInfo,
                                           Flux<SpeedStatistics> peerSpeedStatisticsFlux) {
        this.torrentInfo = torrentInfo;
        this.downloadSpeedFlux = peerSpeedStatisticsFlux
                .flatMap(SpeedStatistics::getDownloadSpeedFlux);

        this.uploadSpeedFlux = peerSpeedStatisticsFlux
                .flatMap(SpeedStatistics::getUploadSpeedFlux);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    @Override
    public double getRateInMillSeconds() {
        return this.rateInMillSeconds;
    }

    public Flux<Double> getDownloadSpeedFlux() {
        return downloadSpeedFlux;
    }

    public Flux<Double> getUploadSpeedFlux() {
        return uploadSpeedFlux;
    }
}
