package main.torrent.status.side.effects;

import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import redux.store.Store;

import java.util.UUID;

public class TorrentFileSystemStatesSideEffects {
    private static Logger logger = LoggerFactory.getLogger(TorrentFileSystemStatesSideEffects.class);

    private Flux<TorrentStatusState> removeFiles$;
    private Flux<TorrentStatusState> removeTorrent$;

    public TorrentFileSystemStatesSideEffects(Store<TorrentStatusState, TorrentStatusAction> store) {
        String transaction = "effects - " + UUID.randomUUID().toString();
//        System.out.println("effects remove file - transaction: " + transaction);
        this.removeFiles$ = store.statesByAction(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS, transaction)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS))
                .doOnNext(__ -> logger.trace("effects remove file - 1: " + __))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP))
                .doOnNext(__ -> logger.trace("effects remove file - 2: " + __))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_DOWNLOAD_WIND_UP))
                .doOnNext(__ -> logger.trace("effects remove file - 3: " + __))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .doOnNext(__ -> logger.trace("effects remove file - 4: " + __))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.REMOVE_FILES_SELF_RESOLVED))
                .doOnNext(__ -> logger.trace("effects remove file - 5: " + __))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.REMOVE_FILES_WIND_UP))
                .publish()
                .autoConnect(0);

        this.removeTorrent$ = store.statesByAction(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS)
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_SEARCHING_PEERS_WIND_UP))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_DOWNLOAD_WIND_UP))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .concatMap(__ -> store.notifyWhen(TorrentStatusAction.REMOVE_TORRENT_SELF_RESOLVED))
                .concatMap(__ -> store.dispatch(TorrentStatusAction.REMOVE_TORRENT_WIND_UP))
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
