package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class StoreNew<S extends State<A>, A> implements Notifier<S, A> {
    private static Logger logger = LoggerFactory.getLogger(StoreNew.class);

    private Function<A, A> getCorrespondingIsProgressAction;
    private FluxSink<RequestForChange<A>> actionsSink;
    private Flux<S> equalStates$;
    private Flux<S> states$;
    private Flux<S> history$;

    public StoreNew(Reducer<S, A> reducer, S defaultState, Function<A, A> getCorrespondingIsProgressAction) {
        this.getCorrespondingIsProgressAction = getCorrespondingIsProgressAction;
        DirectProcessor<S> equalStates$ = DirectProcessor.create();
        FluxSink<S> equalStatesSink = equalStates$.sink(FluxSink.OverflowStrategy.BUFFER);

        this.equalStates$ = equalStates$.replay(1);

        DirectProcessor<RequestForChange<A>> newStates$ = DirectProcessor.create();
        this.actionsSink = newStates$.sink(FluxSink.OverflowStrategy.BUFFER);

        // for debugging
        AtomicInteger transactionCount = new AtomicInteger(0);

        this.states$ = newStates$.scan(defaultState, (S lastState, RequestForChange<A> action) -> {
            logger.info("transaction: " + transactionCount.get() + " - dispatching action: " + action +
                    ". last state: " + lastState);
            S newState = reducer.reducer(lastState, action.getAction());
            if (newState.equals(lastState)) {
                equalStatesSink.next(lastState);
            }
            return newState;
        }).distinctUntilChanged()
                .doOnNext(newState -> logger.info("transaction: " + transactionCount.getAndIncrement() + " - new state: " + newState))
                .replay(1)
                .autoConnect(0);

        this.history$ = this.states$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<S> dispatch(A action) {
        return Mono.just(new RequestForChange<>(action))
                .publishOn(Schedulers.single())
                .doOnNext(requestForChange -> this.actionsSink.next(requestForChange))
                .flatMap(requestForChange ->
                        Flux.merge(this.equalStates$.publishOn(Schedulers.elastic()),
                                this.states$.publishOn(Schedulers.elastic()))
                                .filter(equalState -> equalState.getId().equals(requestForChange.getId()))
                                .take(1)
                                .single())
                .publishOn(Schedulers.parallel());
    }

    public Mono<S> dispatchAsLongNoCancel(A windUpActionToChange) {
        A correspondingIsProgressAction = this.getCorrespondingIsProgressAction.apply(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.states$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .switchMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single();
    }


    @Override
    public Mono<S> getStates$() {
        return this.states$.take(1)
                .single();
    }

    @Override
    public Flux<S> getState$() {
        return this.states$;
    }

    @Override
    public Flux<S> getHistory$() {
        return this.history$;
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
}
