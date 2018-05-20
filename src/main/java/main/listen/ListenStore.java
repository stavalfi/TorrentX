package main.listen;

import main.listen.reducers.ListenReducer;
import main.listen.state.tree.ListenState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ListenStore implements ListenNotifier {

    private FluxSink<ListenState> latestStateSink;
    private Flux<ListenState> latestState$;
    private Flux<ListenState> history$;
    private ListenReducer reducer = new ListenReducer();

    public ListenStore() {
        this.latestState$ = Flux.<ListenState>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(ListenReducer.defaultListenState.get());
        }).replay(1).autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    @Override
    public Mono<ListenState> getLatestState$() {
        return this.latestState$
                .take(1)
                .single();
    }

    @Override
    public Flux<ListenState> getState$() {
        return this.latestState$;
    }

    @Override
    public Flux<ListenState> getHistory$() {
        return this.history$;
    }

    @Override
    public Flux<ListenAction> getAction$() {
        return this.latestState$
                .map(ListenState::getAction);
    }

    public Mono<ListenState> dispatchAsLongNoCancel(ListenAction windUpActionToChange) {
        ListenAction correspondingIsProgressAction = ListenAction.getCorrespondingIsProgressAction(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.latestState$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .flatMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single();
    }

    public Mono<ListenState> dispatch(ListenAction action) {
        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .flatMapMany(lastStatus -> {
                    ListenState newStatus = this.reducer.reducer(lastStatus, action);
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
