package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Store<S extends State<A>, A> implements Notifier<S, A> {
    private static Logger logger = LoggerFactory.getLogger(Store.class);

    private Flux<S> latestState$;
    private Flux<S> history$;
    private Reducer<S, A> reducer;
    private Function<A, A> getCorrespondingIsProgressAction;
    private FluxSink<S> produceNewState;
    private Semaphore semaphore = new Semaphore(1);

    // for debugging
    private AtomicInteger transactionCount = new AtomicInteger(0);

    public Store(Reducer<S, A> reducer, S defaultState, Function<A, A> getCorrespondingIsProgressAction) {
        this.reducer = reducer;
        this.getCorrespondingIsProgressAction = getCorrespondingIsProgressAction;

        this.latestState$ = Flux.<S>create(sink -> {
            this.produceNewState = sink;
            sink.next(defaultState);
        }).replay(1)
                .autoConnect(0);

        this.history$ = this.latestState$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<S> dispatch(A action) {
        return Mono.just(transactionCount.getAndIncrement())
                .publishOn(Schedulers.elastic())
                .flatMap(transactionCount -> {
                    try {
                        this.semaphore.acquire();
                        logger.info("transaction: " + transactionCount + " - acquired semaphore for action: " + action);
                        return Mono.just(transactionCount);
                    } catch (InterruptedException e) {
                        return Mono.error(e);
                    }
                })
                .flatMap(transactionCount -> {
                    logger.info("transaction: " + transactionCount + " - getting the last state.");
                    return getStates$()
                            .flatMap(lastStatus -> {
                                logger.info("transaction: " + transactionCount + " - dispatching action: " + action +
                                        ". last state: " + lastStatus);
                                S newStatus = this.reducer.reducer(lastStatus, action);
                                if (lastStatus.equals(newStatus)) {
                                    logger.info("transaction: " + transactionCount + " - didn't dispatch action: " + action +
                                            " because the new state is illegal or equal to the last state: " + lastStatus);
                                    return Mono.just(lastStatus)
                                            .doOnNext(__ -> this.semaphore.release())
                                            .doOnNext(__ -> logger.info("transaction: " + transactionCount + " - released semaphore for action: " + action));
                                }
                                this.produceNewState.next(newStatus);
                                logger.info("transaction: " + transactionCount + " - dispatched action: " + action +
                                        ". new state: " + newStatus);
                                return getState$()
                                        .filter(newStatus::equals)
                                        .take(1)
                                        .single()
                                        .doOnNext(__ -> this.semaphore.release())
                                        .doOnNext(__ -> logger.info("transaction: " + transactionCount + " - released semaphore for action: " + action));
                            });
                });
    }

    @Override
    public Mono<S> getStates$() {
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

    public Mono<S> dispatchAsLongNoCancel(A windUpActionToChange) {
        A correspondingIsProgressAction = this.getCorrespondingIsProgressAction.apply(windUpActionToChange);
        assert correspondingIsProgressAction != null;

        return this.latestState$
                .takeWhile(listenState -> listenState.fromAction(correspondingIsProgressAction))
                .switchMap(listenState -> dispatch(windUpActionToChange))
                .filter(listenState -> listenState.fromAction(windUpActionToChange))
                .take(1)
                .single();
    }
}
