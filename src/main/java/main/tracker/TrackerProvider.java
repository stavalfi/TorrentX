package main.tracker;

import main.TorrentInfo;
import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class TrackerProvider {
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
                .map(connectResponse -> new TrackerConnection(connectResponse));
    }

    public ConnectableFlux<TrackerConnection> connectToTrackersFlux() {
        return Flux.fromIterable(this.torrentInfo.getTrackerList())
                .flatMap(tracker -> connectToTrackerMono(tracker))
                .publish();
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
