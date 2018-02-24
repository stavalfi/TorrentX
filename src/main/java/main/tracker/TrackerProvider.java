package main.tracker;

import main.TorrentInfo;
import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.stream.Stream;

public class TrackerProvider {
    public static Mono<TrackerConnection> connectToTracker(Tracker tracker) {
        ConnectRequest connectRequest = new ConnectRequest(tracker, 123456);

        Function<ByteBuffer, ConnectResponse> createConnectResponse = (ByteBuffer response) ->
                new ConnectResponse(tracker, response.array());

        return TrackerCommunication.communicate(connectRequest, createConnectResponse)
                .subscribeOn(Schedulers.elastic())
                .onErrorResume(TrackerExceptions.communicationErrors, error -> Mono.empty())
                .map(connectResponse -> new TrackerConnection(connectResponse));
    }

    public static Flux<TrackerConnection> connectToTrackers(Stream<Tracker> trackers) {
        return Flux.fromStream(trackers)
                .flatMap(tracker -> connectToTracker(tracker));
    }

    public static Flux<TrackerConnection> connectToTrackers(TorrentInfo torrentInfo) {
        return TrackerProvider.connectToTrackers(torrentInfo.getTrackerList().stream());
    }
}
