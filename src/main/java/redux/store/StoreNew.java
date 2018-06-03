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

public class StoreNew<STATE_IMPL extends State<ACTION>, ACTION> implements Notifier<STATE_IMPL, ACTION> {
	private static Logger logger = LoggerFactory.getLogger(StoreNew.class);

	private FluxSink<Request<ACTION>> actionsSink;
	private Flux<Result<STATE_IMPL, ACTION>> results$;
	private Flux<STATE_IMPL> states$;

	private FluxSink<Request<ACTION>> emitRequestsSink;
	private BlockingQueue<Request<ACTION>> requestsQueue = new LinkedBlockingQueue<>();

	public StoreNew(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState) {
		Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

		EmitterProcessor<Request<ACTION>> emitRequests = EmitterProcessor.create(Integer.MAX_VALUE);
		this.emitRequestsSink = emitRequests.sink(FluxSink.OverflowStrategy.BUFFER);

		this.results$ = emitRequests
				.scan(initialResult, (Result<STATE_IMPL, ACTION> lastResult, Request<ACTION> request) -> {
					Result<STATE_IMPL, ACTION> result = reducer.reducer(lastResult.getState(), request);
					if (!result.isNewState()) {
						logger.debug("ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
						return result;
					}
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

	private Mono<Result<STATE_IMPL, ACTION>> dispatch2(ACTION action) {
		Request<ACTION> request = new Request<>(action);
		return dispatch(request);
	}

	public Mono<STATE_IMPL> dispatch(ACTION action) {
		return dispatch2(action)
				.map(Result::getState);

	}

	public Mono<STATE_IMPL> dispatchAsLongNoCancel(ACTION action, BiPredicate<ACTION, STATE_IMPL> isCanceled) {
		return states$()
				.takeWhile(stateImpl -> isCanceled.negate().test(action, stateImpl))
				.concatMap(stateImpl -> dispatch(action))
				.filter(stateImpl -> stateImpl.fromAction(action))
				.doOnNext(stateImpl -> logger.debug("we finally succeed to change to " + action + "!"))
				.take(1)
				.switchIfEmpty(latestState$()
						.doOnNext(__ -> logger.debug("we failed to change to: " + action)))
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
