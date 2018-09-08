package main.tracker;

import main.TorrentInfo;
import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class TrackerProvider {
    private static Logger logger = LoggerFactory.getLogger(TrackerProvider.class);

    private TorrentInfo torrentInfo;

    public TrackerProvider(TorrentInfo torrentInfo) {
        this.torrentInfo = torrentInfo;
    }

    public Mono<TrackerConnection> connectToTrackerMono(Tracker tracker) {
        ConnectRequest connectRequest = new ConnectRequest(tracker, 123456);

        Function<ByteBuffer, ConnectResponse> createConnectResponse = (ByteBuffer response) ->
                new ConnectResponse(tracker, response.array());
        return TrackerCommunication.communicateMono(connectRequest, createConnectResponse)
                .onErrorResume(TrackerExceptions.communicationErrors, error -> Mono.empty())
                .map(TrackerConnection::new)
                .doOnNext(trackerConnection -> logger.info("connected to tracker: " + trackerConnection));
    }

    public Flux<TrackerConnection> connectToTrackersFlux() {
        return Flux.fromIterable(this.torrentInfo.getTrackerList())
                .flatMap(this::connectToTrackerMono);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
