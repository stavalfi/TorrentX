package main.peer;

import main.tracker.Tracker;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

public class PeersProvider {
    public static Flux<Peer> peers(Stream<Tracker> trackers) {
        return Flux.error(new Exception());
    }
}
