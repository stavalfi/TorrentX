package main.torrent.status;

import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatusNotifier {
    Mono<TorrentStatusState> getLatestState$();

    Flux<TorrentStatusState> getState$();

    Flux<TorrentStatusState> getHistory$();
}
