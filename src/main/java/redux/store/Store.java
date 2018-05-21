package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redux.state.State;
import redux.reducer.IReducer;

import java.util.function.Function;

public class Store<S extends State<A>, A> implements Notifier<S, A> {
    private static Logger logger = LoggerFactory.getLogger(Store.class);

    private S defaultState;

    private FluxSink<S> latestStateSink;
    private Flux<S> latestState$;
    private Flux<S> history$;
    private IReducer<S, A> reducer;
    private Function<A, A> getCorrespondingIsProgressAction;

    public Store(IReducer<S, A> reducer, S defaultState, Function<A, A> getCorrespondingIsProgressAction) {
        this.latestState$ = Flux.<S>create(sink -> {
            this.latestStateSink = sink;
            this.latestStateSink.next(defaultState);
        }).doOnNext(listenerState -> logger.debug("new state: " + listenerState))
                .replay(1)
                .autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    @Override
    public Mono<S> getLatestState$() {
        return this.latestState$
                .take(1)
                .single();
    }

    @Override
    public Flux<S> getState$() {
        return this.latestState$;
    }

    @Override
    public Flux<S> getHistory$() {
        return this.history$.publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<S> getByAction$(A action) {
        return getState$().filter(listenerState -> listenerState.getAction().equals(action));
    }

    @Override
    public Mono<S> notifyWhen(A when) {
        return getState$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single();
    }

    @Override
    public <T> Mono<T> notifyWhen(A when, T mapTo) {
        return getState$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single()
                .map(listenerState -> mapTo);
    }

    public Mono<S> dispatchAsLongNoCancel(A windUpActionToChange) {
        A correspondingIsProgressAction = this.getCorrespondingIsProgressAction.apply(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.latestState$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .flatMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single();
    }

    public Mono<S> dispatch(A action) {
        return this.latestState$
                .publishOn(Schedulers.single())
                .take(1)
                .single()
                .flatMapMany(lastStatus -> {
                    System.out.println("123-1");
                    S newStatus = this.reducer.reducer(lastStatus, action);
                    if (lastStatus.equals(newStatus))
                        return getLatestState$();
                    System.out.println("123-2");
                    this.latestStateSink.next(newStatus);
                    return this.latestState$.filter(status -> status.equals(newStatus));
                })
                .take(1)
                .single()
                .publishOn(Schedulers.parallel());
    }
}
