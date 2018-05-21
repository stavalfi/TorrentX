package main.listen;

import main.listen.reducers.ListenerReducer;
import main.listen.state.tree.ListenerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ListenerStore implements ListenerNotifier {
    private static Logger logger = LoggerFactory.getLogger(ListenerStore.class);

    private FluxSink<ListenerState> latestStateSink;
    private Flux<ListenerState> latestState$;
    private Flux<ListenerState> history$;
    private ListenerReducer reducer = new ListenerReducer();

    public ListenerStore() {
        this.latestState$ = Flux.<ListenerState>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(ListenerReducer.defaultListenState.get());
        })
                .doOnNext(listenerState -> logger.debug("new state: " + listenerState))
                .replay(1)
                .autoConnect(0);

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
        return this.history$.publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<ListenerState> getByAction$(ListenerAction action) {
        return getState$().filter(listenerState -> listenerState.getAction().equals(action));
    }

    @Override
    public Mono<ListenerState> notifyWhen(ListenerAction when) {
        return getState$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single();
    }

    @Override
    public <T> Mono<T> notifyWhen(ListenerAction when, T mapTo) {
        return getState$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single()
                .map(listenerState -> mapTo);
    }

    public Mono<ListenerState> dispatchAsLongNoCancel(ListenerAction windUpActionToChange) {
        ListenerAction correspondingIsProgressAction = ListenerAction.getCorrespondingIsProgressAction(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.latestState$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .flatMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single()
                .doOnNext(listenerState -> System.out.println("hi"));
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
