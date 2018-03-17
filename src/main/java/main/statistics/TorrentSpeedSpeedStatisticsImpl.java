package main.statistics;

import main.TorrentInfo;
import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;

public class TorrentSpeedSpeedStatisticsImpl implements SpeedStatistics {

    private TorrentInfo torrentInfo;

    private Flux<Double> downloadSpeedFlux;
    private Flux<Double> uploadSpeedFlux;


    public TorrentSpeedSpeedStatisticsImpl(TorrentInfo torrentInfo,
                                           Flux<? extends PeerMessage> receivedMessageMessages,
                                           Flux<? extends PeerMessage> sentSentMessages) {
        this.torrentInfo = torrentInfo;
        this.downloadSpeedFlux = receivedMessageMessages
                .map(PeerMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);

        this.uploadSpeedFlux = sentSentMessages
                .map(PeerMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);
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

    public Flux<Double> getDownloadSpeedFlux() {
        return downloadSpeedFlux;
    }

    public Flux<Double> getUploadSpeedFlux() {
        return uploadSpeedFlux;
    }
}
