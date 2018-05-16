package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

public class TorrentFileSystemStatesSideEffects {
    private Flux<TorrentStatusState> removeFiles$;
    private Flux<TorrentStatusState> removeTorrent$;

    public TorrentFileSystemStatesSideEffects(TorrentInfo torrentInfo,
                                              TorrentStatusStore store) {
        this.removeFiles$ = store.getAction$(torrentInfo)
                .filter(Action.REMOVE_FILES_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_UPLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.REMOVE_FILES_WIND_UP))
                .publish()
                .autoConnect(0);

        this.removeTorrent$ = store.getAction$(torrentInfo)
                .filter(Action.REMOVE_TORRENT_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatch(torrentInfo, Action.PAUSE_UPLOAD_IN_PROGRESS))
                .flatMap(__ -> store.dispatchAsLongNoCancel(torrentInfo, Action.REMOVE_TORRENT_WIND_UP))
                .publish()
                .autoConnect(0);
    }

    public Flux<TorrentStatusState> getRemoveFiles$() {
        return removeFiles$;
    }

    public Flux<TorrentStatusState> getRemoveTorrent$() {
        return removeTorrent$;
    }
}
