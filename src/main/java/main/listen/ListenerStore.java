package main.listen;

import main.listen.reducers.ListenerReducer;
import main.listen.state.tree.ListenerState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ListenerStore implements ListenerNotifier {

    private FluxSink<ListenerState> latestStateSink;
    private Flux<ListenerState> latestState$;
    private Flux<ListenerState> history$;
    private ListenerReducer reducer = new ListenerReducer();

    public ListenerStore() {
        this.latestState$ = Flux.<ListenerState>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(ListenerReducer.defaultListenState.get());
        }).replay(1).autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    @Override
    public Mono<ListenerState> getLatestState$() {
        return this.latestState$
                .take(1)
                .single();
    }

    @Override
    public Flux<ListenerState> getState$() {
        return this.latestState$;
    }

    @Override
    public Flux<ListenerState> getHistory$() {
        return this.history$;
    }

    @Override
    public Flux<ListenerAction> getAction$() {
        return this.latestState$
                .map(ListenerState::getAction);
    }

    public Mono<ListenerState> dispatchAsLongNoCancel(ListenerAction windUpActionToChange) {
        ListenerAction correspondingIsProgressAction = ListenerAction.getCorrespondingIsProgressAction(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.latestState$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .flatMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single();
    }

    public Mono<ListenerState> dispatch(ListenerAction action) {
        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .flatMapMany(lastStatus -> {
                    ListenerState newStatus = this.reducer.reducer(lastStatus, action);
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
