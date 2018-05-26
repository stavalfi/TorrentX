package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class StoreNew<STATE_IMPL extends State<ACTION>, ACTION> implements Notifier<STATE_IMPL, ACTION> {
    private static Logger logger = LoggerFactory.getLogger(StoreNew.class);

    private Function<ACTION, ACTION> getCorrespondingIsProgressAction;
    private FluxSink<Request<ACTION>> actionsSink;
    private Flux<Request<ACTION>> ignoredRequests$;
    private Flux<STATE_IMPL> states$;
    private Flux<STATE_IMPL> history$;

    public StoreNew(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState,
                    Function<ACTION, ACTION> getCorrespondingIsProgressAction) {
        this.getCorrespondingIsProgressAction = getCorrespondingIsProgressAction;
        EmitterProcessor<Request<ACTION>> ignoreRequests = EmitterProcessor.create();
        FluxSink<Request<ACTION>> ignoreRequestsSink = ignoreRequests.sink(FluxSink.OverflowStrategy.BUFFER);

        this.ignoredRequests$ = ignoreRequests
                .doOnNext(request -> logger.debug("4: " + request + "\n"))
                .replay(1)
                .autoConnect(0);

        EmitterProcessor<Request<ACTION>> newStates$ = EmitterProcessor.create();
        this.actionsSink = newStates$.sink(FluxSink.OverflowStrategy.BUFFER);

        this.states$ = newStates$
                .doOnNext(request -> logger.debug("3: " + request + "\n"))
                .index()
                .scan(defaultState, (STATE_IMPL lastState, Tuple2<Long, Request<ACTION>> request) -> {
                    long transactionCount = request.getT1();
                    //logger.debug("transaction: " + transactionCount + " - dispatching request: " + request.getT2() +
                    // ". last state: " + lastState + "\n");
                    STATE_IMPL newState = reducer.reducer(lastState, request.getT2());
                    if (newState.equals(lastState)) {
                        ignoreRequestsSink.next(request.getT2());
                        //logger.debug("transaction: " + transactionCount + " - ignored request: " + request.getT2() + "\n");
                    } else {
                        //logger.info("transaction: " + transactionCount + " - new state: " + newState + "\n");
                    }
                    return newState;
                })
                .distinctUntilChanged()
                .doOnNext(request -> logger.debug("4: " + request + "\n"))
                .replay(1)
                .autoConnect(0);

        this.history$ = this.states$.replay(10) // how much statuses to save.
                .autoConnect(0);
    }

    public Mono<STATE_IMPL> dispatch(ACTION action) {
        logger.debug("getting ready for dispatching action: " + action);
        Request<ACTION> request = new Request<>(action);
        Mono<STATE_IMPL> result = Flux.merge(this.ignoredRequests$.publishOn(Schedulers.elastic())
                        .map(ignoredRequest -> ignoredRequest.getId())
                        .filter(ignoredRequestId -> ignoredRequestId.equals(request.id))
                //.doOnNext(__ -> logger.debug("finally - ignored request: " + request + "\n"))
                ,
                this.states$.publishOn(Schedulers.elastic())
                        .map(newState -> newState.getId())
                        .filter(ignoredRequestId -> ignoredRequestId.equals(request.id))
                //.doOnNext(__ -> logger.debug("finally - accepted request: " + request + "\n"))
        )
                .doOnNext(__ -> logger.debug("5: " + request + "\n"))
                .map(__ -> request)
                .limitRequest(1)
                .replay(1)
                .autoConnect(0)
                .flatMap(__ -> this.states$.publishOn(Schedulers.elastic()))
                .take(1)
                .single()
                .doOnNext(___ -> logger.debug("6: " + request + "\n"))
                .doOnNext(___ -> logger.debug("7: " + request + "\n"));

        return Mono.just(request)
                .doOnNext(__ -> logger.debug("1: " + request + "\n"))
                .doOnNext(__ -> logger.debug("2: " + request + "\n"))
                .doOnNext(__ -> this.actionsSink.next(request))
                .flatMap(__ -> result);
    }

    public Mono<STATE_IMPL> dispatchAsLongNoCancel(ACTION action, BiPredicate<ACTION, STATE_IMPL> isCanceled) {
        return this.states$.publishOn(Schedulers.elastic())
                .doOnNext(stateImpl -> logger.debug("is going to stop change to " + action + "? :" + stateImpl))
                .takeWhile(stateImpl -> isCanceled.negate().test(action, stateImpl))
                .concatMap(stateImpl -> dispatch(action))
                .doOnNext(stateImpl -> logger.debug("did we succeed to change to " + action + "? "
                        + stateImpl.fromAction(action) + ":" + stateImpl))
                .filter(stateImpl -> stateImpl.fromAction(action))
                .doOnNext(stateImpl -> logger.debug("we finally succeed to change to " + action + "!"))
                .take(1)
                .switchIfEmpty(latestState$()
                        .doOnNext(__ -> logger.debug("we failed to change to: " + action)))
                .single();
    }

    @Override
    public Mono<STATE_IMPL> latestState$() {
        return this.states$.take(1)
                .single()
                .publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<STATE_IMPL> states$() {
        return this.states$
                .publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<STATE_IMPL> statesHistory() {
        return this.history$
                .publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<STATE_IMPL> statesByAction(ACTION action) {
        return this.states$.publishOn(Schedulers.elastic())
                .filter(stateImpl -> stateImpl.getAction().equals(action));
    }

    @Override
    public Mono<STATE_IMPL> notifyWhen(ACTION when) {
        return states$().publishOn(Schedulers.elastic())
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .single();
    }

    @Override
    public <T> Mono<T> notifyWhen(ACTION when, T mapTo) {
        return states$().publishOn(Schedulers.elastic())
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .single()
                .map(stateImpl -> mapTo);
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
