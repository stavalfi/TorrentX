package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.peer.PeersListener;
import main.torrent.status.Action;
import main.torrent.status.TorrentStatusStore;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;

public class TorrentStatusSideEffects {
    private TorrentInfo torrentInfo;
    private TorrentStatusStore torrentStatusStore;
    private PeersListener peersListener;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;

    private Flux<Action> action$ = this.torrentStatusStore.getState$()
            .map(TorrentStatusState::getAction);

    private Flux<TorrentStatusState> searchPeersEffect$ =
            this.action$.filter(Action.START_SEARCHING_PEERS_IN_PROGRESS::equals)
                    .flatMap(__ -> torrentStatusStore.changeState(Action.START_SEARCHING_PEERS_WIND_UP))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.RESUME_SEARCHING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> notifyUntilChange(Action.COMPLETED_DOWNLOADING_WIND_UP, Action.COMPLETED_DOWNLOADING_IN_PROGRESS))
                    .publish()
                    .autoConnect(0);
    private Flux<TorrentStatusState> downloadCompleteEffect$ =
            this.action$.filter(Action.COMPLETED_DOWNLOADING_IN_PROGRESS::equals)
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> notifyUntilChange(Action.COMPLETED_DOWNLOADING_WIND_UP, Action.COMPLETED_DOWNLOADING_IN_PROGRESS))
                    .publish()
                    .autoConnect(0);

    private Flux<TorrentStatusState> torrentRemoveEffect$ =
            this.action$.filter(Action.REMOVE_TORRENT_IN_PROGRESS::equals)
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_UPLOAD_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                    .flatMap(__ -> this.fileSystemLink.deleteActiveTorrentOnlyMono())
                    .flatMap(__ -> notifyUntilChange(Action.REMOVE_TORRENT_WIND_UP, Action.REMOVE_TORRENT_IN_PROGRESS))
                    .publish()
                    .autoConnect(0);

    private Flux<TorrentStatusState> filesRemoveEffect$ =
            this.action$.filter(Action.REMOVE_TORRENT_IN_PROGRESS::equals)
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_UPLOAD_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_SEARCHING_PEERS_IN_PROGRESS))
                    .flatMap(__ -> torrentStatusStore.changeState(Action.PAUSE_DOWNLOAD_IN_PROGRESS))
                    .flatMap(__ -> this.fileSystemLink.deleteFileOnlyMono())
                    .flatMap(__ -> notifyUntilChange(Action.REMOVE_TORRENT_WIND_UP, Action.REMOVE_TORRENT_IN_PROGRESS))
                    .publish()
                    .autoConnect(0);

    public TorrentStatusSideEffects(TorrentInfo torrentInfo,
                                    TorrentStatusStore torrentStatusStore,
                                    PeersListener peersListener,
                                    FileSystemLink fileSystemLink,
                                    BittorrentAlgorithm bittorrentAlgorithm) {
        this.torrentInfo = torrentInfo;
        this.torrentStatusStore = torrentStatusStore;
        this.peersListener = peersListener;
        this.fileSystemLink = fileSystemLink;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
    }

    private Flux<TorrentStatusState> notifyUntilChange(Action change, Action resumeIf) {
        return this.torrentStatusStore.getState$()
                .takeUntil(torrentStatusState -> torrentStatusState.fromAction(resumeIf))
                .flatMap(torrentStatusState -> torrentStatusStore.changeState(change));
    }
}
