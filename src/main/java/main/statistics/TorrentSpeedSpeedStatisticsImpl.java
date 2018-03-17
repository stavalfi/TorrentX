package main.statistics;

import main.TorrentInfo;
import reactor.core.publisher.Flux;

public class TorrentSpeedSpeedStatisticsImpl implements SpeedStatistics {

    private TorrentInfo torrentInfo;

    private Flux<Double> torrentDownloadSpeedFlux;
    private Flux<Double> torrentUploadSpeedFlux;

    public TorrentSpeedSpeedStatisticsImpl(TorrentInfo torrentInfo,
                                           Flux<SpeedStatistics> peerSpeedStatisticsFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentDownloadSpeedFlux = peerSpeedStatisticsFlux
                .flatMap(SpeedStatistics::getDownloadSpeedFlux);

        this.torrentUploadSpeedFlux = peerSpeedStatisticsFlux
                .flatMap(SpeedStatistics::getUploadSpeedFlux);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Flux<Double> getDownloadSpeedFlux() {
        return torrentDownloadSpeedFlux;
    }

    public Flux<Double> getUploadSpeedFlux() {
        return torrentUploadSpeedFlux;
    }
}
