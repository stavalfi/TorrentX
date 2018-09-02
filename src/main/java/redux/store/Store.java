package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.function.BiPredicate;

public class Store<STATE_IMPL extends State<ACTION>, ACTION> implements Notifier<STATE_IMPL, ACTION> {
    private static Logger logger = LoggerFactory.getLogger(Store.class);
    private String identifier;

    private Flux<Result<STATE_IMPL, ACTION>> results$;
    private Flux<STATE_IMPL> states$;

    private FluxSink<Request<ACTION>> emitRequestsSink;
    private Scheduler pullerScheduler;

    public Store(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState, String identifier) {
        this.identifier = identifier;
        this.pullerScheduler = Schedulers.newSingle(this.identifier + " - PULLER");
        Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

        UnicastProcessor<Request<ACTION>> requests$ = UnicastProcessor.create();
        this.emitRequestsSink = requests$.sink();

        this.results$ = requests$.subscribeOn(this.pullerScheduler)
                .doOnNext(request -> logger.info(this.identifier + " - start inspecting request: " + request))
                .scan(initialResult, (Result<STATE_IMPL, ACTION> lastResult, Request<ACTION> request) -> {
                    logger.trace(this.identifier + " - start processing request: " + request + ", current state: " + lastResult.getState());
                    Result<STATE_IMPL, ACTION> result = reducer.reducer(lastResult.getState(), request);
                    if (!result.isNewState())
                        logger.debug(this.identifier + " - ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
                    else
                        logger.debug(this.identifier + " - passed - request: " + request + " new state: " + result.getState() + "\n");
                    return result;
                })
                .replay()
                .autoConnect(0);

        this.states$ = this.results$
                .doOnNext(result -> logger.trace(this.identifier + " - analyzing result: " + result))
                .map(Result::getState)
                .distinctUntilChanged()
                .doOnNext(state -> logger.debug(this.identifier + " - new state: " + state))
                .replay(1)
                .autoConnect(0);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void dispatchNonBlocking(ACTION action) {
        Request<ACTION> request = new Request<>(action);
        this.emitRequestsSink.next(request);
    }

    public void dispatchNonBlocking(Request<ACTION> request) {
        this.emitRequestsSink.next(request);
    }

    public Mono<Result<STATE_IMPL, ACTION>> dispatch(Request<ACTION> request) {
        Flux<Result<STATE_IMPL, ACTION>> resultFlux = this.results$
                .filter(result -> result.getRequest().equals(request))
                .take(1)
                .replay()
                .autoConnect(0);
        this.emitRequestsSink.next(request);
        return resultFlux.take(1)
                .singleOrEmpty();
    }

    public Mono<STATE_IMPL> dispatch(ACTION action) {
        Request<ACTION> request = new Request<>(action);
        return dispatch(request)
                .map(Result::getState);

    }

    public Mono<STATE_IMPL> tryDispatchUntil(ACTION action, BiPredicate<ACTION, STATE_IMPL> until) {
        return states$()
                .takeWhile(stateImpl -> until.test(action, stateImpl))
                .concatMap(stateImpl -> dispatch(action))
                .filter(stateImpl -> stateImpl.fromAction(action))
                .take(1)
                .switchIfEmpty(latestState$())
                .singleOrEmpty();
    }

    @Override
    public Mono<STATE_IMPL> latestState$() {
        return this.states$
                .take(1)
                .singleOrEmpty();
    }

    @Override
    public Flux<STATE_IMPL> states$() {
        return this.states$;
    }

    @Override
    public Flux<STATE_IMPL> statesHistory() {
        return null;
    }

    @Override
    public Flux<STATE_IMPL> statesByAction(ACTION action) {
        return states$().filter(stateImpl -> stateImpl.getAction().equals(action));
    }

    @Override
    public Mono<STATE_IMPL> notifyWhen(ACTION when) {
        return states$()
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .singleOrEmpty();
    }

    @Override
    public <T> Mono<T> notifyWhen(ACTION when, T mapTo) {
        return states$()
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .singleOrEmpty()
                .map(stateImpl -> mapTo);
    }

    public void dispose() {
        this.emitRequestsSink.complete();
        this.pullerScheduler.dispose();
    }
}