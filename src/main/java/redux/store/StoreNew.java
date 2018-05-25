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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class StoreNew<S extends State<A>, A> implements Notifier<S, A> {
    private static Logger logger = LoggerFactory.getLogger(StoreNew.class);

    private Function<A, A> getCorrespondingIsProgressAction;
    private FluxSink<Request<A>> actionsSink;
    private Flux<Request<A>> ignoredRequests$;
    private Flux<S> states$;
    private Flux<S> history$;

    public StoreNew(Reducer<S, A> reducer, S defaultState, Function<A, A> getCorrespondingIsProgressAction) {
        this.getCorrespondingIsProgressAction = getCorrespondingIsProgressAction;
        DirectProcessor<Request<A>> ignoreRequests = DirectProcessor.create();
        FluxSink<Request<A>> ignoreRequestsSink = ignoreRequests.sink(FluxSink.OverflowStrategy.BUFFER);

        this.ignoredRequests$ = ignoreRequests.replay(1).autoConnect(0);

        DirectProcessor<Request<A>> newStates$ = DirectProcessor.create();
        this.actionsSink = newStates$.sink(FluxSink.OverflowStrategy.BUFFER);

        // for debugging
        AtomicInteger transactionCount = new AtomicInteger(0);

        this.states$ = newStates$.scan(defaultState, (S lastState, Request<A> request) -> {
            logger.info("transaction: " + transactionCount.get() + " - dispatching request: " + request +
                    ". last state: " + lastState);
            S newState = reducer.reducer(lastState, request);
            if (newState.equals(lastState)) {
                logger.info("transaction: " + transactionCount.get() + " - ignored request: " + request);
                ignoreRequestsSink.next(request);
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
        return Mono.just(new Request<>(action))
                .publishOn(Schedulers.single())
                .doOnNext(request -> this.actionsSink.next(request))
                .map(Request::getId)
                .flatMapMany(requestId ->
                        Flux.merge(this.ignoredRequests$.subscribeOn(Schedulers.elastic())
                                        .map(ignoredRequest -> ignoredRequest.getId()),
                                this.states$.subscribeOn(Schedulers.elastic())
                                        .map(newState -> newState.getId()))
                                .filter(ignoredRequestId -> ignoredRequestId.equals(requestId)))
                .flatMap(__ -> latestState$())
                .take(1)
                .single()
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
    public Mono<S> latestState$() {
        return this.states$.take(1)
                .single();
    }

    @Override
    public Flux<S> states$() {
        return this.states$;
    }

    @Override
    public Flux<S> statesHistory() {
        return this.history$;
    }

    @Override
    public Flux<S> statesByAction(A action) {
        return states$().filter(listenerState -> listenerState.getAction().equals(action));
    }

    @Override
    public Mono<S> notifyWhen(A when) {
        return states$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single();
    }

    @Override
    public <T> Mono<T> notifyWhen(A when, T mapTo) {
        return states$()
                .filter(listenerState -> listenerState.fromAction(when))
                .take(1)
                .single()
                .map(listenerState -> mapTo);
    }

    public static class Request<A> {
        private A action;
        private String id;

        private Request(A action) {
            this.action = action;
            this.id = UUID.randomUUID().toString();
        }

        public A getAction() {
            return action;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State<?> state = (State<?>) o;
            return Objects.equals(getId(), state.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId());
        }

        @Override
        public String toString() {
            return "Request{" +
                    "action=" + action +
                    ", id='" + id + '\'' +
                    '}';
        }
    }
}
