package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiPredicate;

public class Store<STATE_IMPL extends State<ACTION>, ACTION> implements Notifier<STATE_IMPL, ACTION> {
    private static Logger logger = LoggerFactory.getLogger(Store.class);

    private Flux<Result<STATE_IMPL, ACTION>> results$;
    private Flux<STATE_IMPL> states$;
    private BlockingQueue<Request<ACTION>> requestsQueue = new LinkedBlockingQueue<>();

    public Store(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState) {
        Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

        this.results$ = Flux.create((FluxSink<Request<ACTION>> sink) -> {
            while (true) {
                try {
                    Request<ACTION> request = this.requestsQueue.take();
                    sink.next(request);
                } catch (InterruptedException e) {
                    sink.error(e);
                    return;
                }
            }
        }).subscribeOn(Schedulers.elastic())
                .scan(initialResult, (Result<STATE_IMPL, ACTION> lastResult, Request<ACTION> request) -> {
                    Result<STATE_IMPL, ACTION> result = reducer.reducer(lastResult.getState(), request);
                    if (!result.isNewState())
                        logger.debug("ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
                    else
                        logger.debug("passed - request: " + request + " new state: " + result.getState() + "\n");
                    return result;
                })
                .replay()
                .autoConnect(0);

        this.states$ = this.results$
                .doOnNext(result -> logger.debug("analyzing result: " + result))
                .map(Result::getState)
                .distinctUntilChanged()
                .doOnNext(state -> logger.debug("new state1: " + state))
                .replay(1)
                .autoConnect(0)
                .doOnNext(state -> logger.debug("new state2: " + state));
    }

    public void dispatchNonBlocking(ACTION action) {
        Request<ACTION> request = new Request<>(action);
        logger.debug("getting ready for dispatching request: " + request);
        try {
            this.requestsQueue.put(request);
        } catch (InterruptedException e) {
            // TODO: do something with this
            e.printStackTrace();
        }
    }

    public Mono<Result<STATE_IMPL, ACTION>> dispatch(Request<ACTION> request) {
        logger.debug("getting ready for dispatching request: " + request);
        Flux<Result<STATE_IMPL, ACTION>> resultFlux = this.results$
                .filter(result -> result.getRequest().equals(request))
                .take(1)
                .replay()
                .autoConnect(0);
        try {
            this.requestsQueue.put(request);
        } catch (InterruptedException e) {
            return Mono.error(e);
        }
        return resultFlux.take(1)
                .single();
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
                .single();
    }

    @Override
    public Mono<STATE_IMPL> latestState$() {
        return this.states$
                .take(1)
                .single();
    }

    @Override
    public Flux<STATE_IMPL> states$() {
        return this.states$
                .publishOn(Schedulers.elastic());
    }

    @Override
    public Flux<STATE_IMPL> statesHistory() {
        return null;
    }

    @Override
    public Flux<STATE_IMPL> statesByAction(ACTION action) {
        logger.debug("statesByAction - 0: " + action);
        return states$()
                .doOnNext(__ -> logger.debug("statesByAction - 1: " + action + " - state: " + __))
                .filter(stateImpl -> stateImpl.getAction().equals(action));
    }

    public Flux<STATE_IMPL> statesByAction(ACTION action, String transaction) {
        logger.debug("statesByAction - transaction:" + transaction + " - 0:" + action);
        return states$()
                .doOnSubscribe(__ -> logger.debug("statesByAction - transaction:" + transaction + " - 1:" + action))
                .doOnNext(__ -> logger.debug("statesByAction - transaction:" + transaction + " - 2:" + action + " - state: " + __))
                .filter(stateImpl -> stateImpl.getAction().equals(action))
                .doOnNext(__ -> logger.debug("statesByAction - transaction:" + transaction + " - 3:" + action + " - state: " + __));
    }

    @Override
    public Mono<STATE_IMPL> notifyWhen(ACTION when) {
        return states$()
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .single();
    }

    @Override
    public <T> Mono<T> notifyWhen(ACTION when, T mapTo) {
        return states$()
                .filter(stateImpl -> stateImpl.fromAction(when))
                .take(1)
                .single()
                .map(stateImpl -> mapTo);
    }
}