package main.torrent.status;

import main.TorrentInfo;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatusNotifier {
    Mono<TorrentStatusState> getLatestState$(TorrentInfo torrentInfo);

    Flux<TorrentStatusState> getState$(TorrentInfo torrentInfo);

    Flux<TorrentStatusState> getHistory$(TorrentInfo torrentInfo);

    Flux<Action> getAction$(TorrentInfo torrentInfo);
}
