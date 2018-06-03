package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;
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

	private FluxSink<Request<ACTION>> actionsSink;
	private Flux<Result<STATE_IMPL, ACTION>> results$;
	private Flux<STATE_IMPL> states$;

	private FluxSink<Request<ACTION>> emitRequestsSink;
	private BlockingQueue<Request<ACTION>> requestsQueue = new LinkedBlockingQueue<>();

	public Store(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState) {
		Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

		EmitterProcessor<Request<ACTION>> emitRequests = EmitterProcessor.create(Integer.MAX_VALUE);
		this.emitRequestsSink = emitRequests.sink(FluxSink.OverflowStrategy.BUFFER);

		this.results$ = emitRequests
				.scan(initialResult, (Result<STATE_IMPL, ACTION> lastResult, Request<ACTION> request) -> {
					Result<STATE_IMPL, ACTION> result = reducer.reducer(lastResult.getState(), request);
					if (result.isNewState())
						logger.debug("ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
					else
						logger.debug("passed - request: " + request + " new state: " + result.getState() + "\n");
					return result;
				})
				.replay()
				.autoConnect(0);

		this.states$ = this.results$
				.map(Result::getState)
				.distinctUntilChanged()
				.replay(1)
				.autoConnect(0);
	}

	public void dispatchNonBlocking(ACTION action) {
		Request<ACTION> request = new Request<>(action);
		logger.debug("getting ready for dispatching request: " + request);
		this.emitRequestsSink.next(request);
	}

	public Mono<Result<STATE_IMPL, ACTION>> dispatch(Request<ACTION> request) {
		logger.debug("getting ready for dispatching request: " + request);
		Flux<Result<STATE_IMPL, ACTION>> resultFlux = this.results$
				.filter(result -> result.getRequest().equals(request))
				.take(1)
				.replay()
				.autoConnect(0);
		this.emitRequestsSink.next(request);
		return resultFlux.take(1)
				.single();
	}

	public Mono<STATE_IMPL> dispatch(ACTION action) {
		Request<ACTION> request = new Request<>(action);
		return dispatch(request)
				.map(Result::getState);

	}

	public Mono<STATE_IMPL> dispatchAsLongNoCancel(ACTION action, BiPredicate<ACTION, STATE_IMPL> isCanceled) {
		return states$()
				.takeWhile(stateImpl -> isCanceled.negate().test(action, stateImpl))
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
		return states$()
				.filter(stateImpl -> stateImpl.getAction().equals(action));
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