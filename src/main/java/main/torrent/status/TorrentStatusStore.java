package main.torrent.status;

import main.torrent.status.reducers.Reducer;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// TODO: change class name to TorrentStatusChanger
public class TorrentStatusStore implements StatusNotifier {

    private FluxSink<TorrentStatusState> latestStateSink;
    private Flux<TorrentStatusState> latestState$;
    private Flux<TorrentStatusState> history$;
    private Reducer reducer = new Reducer();

    public TorrentStatusStore(TorrentStatusState initialTorrentStatusState) {
        this.latestState$ = Flux.<TorrentStatusState>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(initialTorrentStatusState);
        })
                .replay(1)
                .autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<TorrentStatusState> getLatestState$() {
        return this.latestState$.take(1)
                .single();
    }

    public Flux<TorrentStatusState> getState$() {
        return this.latestState$;
    }

    public Flux<TorrentStatusState> getHistory$() {
        return this.history$;
    }

    public Mono<TorrentStatusState> changeState(Action action) {
        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .flatMapMany(lastStatus -> {
                    TorrentStatusState newStatus = this.reducer.reducer(lastStatus, action);
                    if (lastStatus.equals(newStatus))
                        return getLatestState$();
                    this.latestStateSink.next(newStatus);
                    return this.latestState$.filter(status -> status.equals(newStatus));
                })
                .take(1)
                .single()
                .publishOn(Schedulers.parallel());
    }
}
