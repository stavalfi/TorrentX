package main.torrent.status;

import main.TorrentInfo;
import main.torrent.status.reducers.Reducer;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TorrentStatusStore implements StatusNotifier {

    private FluxSink<TorrentStatusState> latestStateSink;
    private Flux<TorrentStatusState> latestState$;
    private Flux<TorrentStatusState> history$;
    private Reducer reducer = new Reducer();

    public TorrentStatusStore() {
        this.latestState$ = Flux.<TorrentStatusState>create(sink -> {
            this.latestStateSink = sink;
        }).replay(1).autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<TorrentStatusState> getLatestState$(TorrentInfo torrentInfo) {
        return this.latestState$
                .filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo))
                .take(1)
                .single();
    }

    public Flux<TorrentStatusState> getState$(TorrentInfo torrentInfo) {
        return this.latestState$.filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo));
    }

    public Flux<TorrentStatusState> getHistory$(TorrentInfo torrentInfo) {
        return this.history$.filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo));
    }

    public Flux<Action> getAction$(TorrentInfo torrentInfo) {
        return this.latestState$
                .filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo))
                .map(TorrentStatusState::getAction);
    }

    public Mono<TorrentStatusState> notifyUntilChange(TorrentInfo torrentInfo, Action change, Action resumeIf) {
        return this.latestState$.filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo))
                .takeUntil(torrentStatusState -> torrentStatusState.fromAction(resumeIf))
                .flatMap(torrentStatusState -> changeState(torrentInfo, change))
                .take(1)
                .single();
    }

    public Mono<TorrentStatusState> initializeState(TorrentStatusState initialTorrentStatusState) {
        assert initialTorrentStatusState.getAction().equals(Action.INITIALIZE);

        return Mono.just(initialTorrentStatusState)
                .publishOn(Schedulers.single())
                .doOnNext(__ -> this.latestStateSink.next(initialTorrentStatusState))
                .flatMapMany(__ -> this.latestState$)
                .filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(initialTorrentStatusState.getTorrentInfo()))
                .filter(status -> status.equals(initialTorrentStatusState))
                .take(1)
                .single()
                .publishOn(Schedulers.parallel());
    }

    public Mono<TorrentStatusState> changeState(TorrentInfo torrentInfo, Action action) {
        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .flatMapMany(lastStatus -> {
                    TorrentStatusState newStatus = this.reducer.reducer(torrentInfo, lastStatus, action);
                    if (lastStatus.equals(newStatus))
                        return getLatestState$(torrentInfo);
                    this.latestStateSink.next(newStatus);
                    return this.latestState$.filter(torrentStatusState -> torrentStatusState.getTorrentInfo().equals(torrentInfo))
                            .filter(status -> status.equals(newStatus));
                })
                .take(1)
                .single()
                .publishOn(Schedulers.parallel());
    }
}
