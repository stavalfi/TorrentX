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

public class StoreNew<STATE_IMPL extends State<ACTION>, ACTION> implements Notifier<STATE_IMPL, ACTION> {
	private static Logger logger = LoggerFactory.getLogger(StoreNew.class);

	private FluxSink<Request<ACTION>> actionsSink;
	private Flux<Result<STATE_IMPL, ACTION>> results$;
	private Flux<STATE_IMPL> states$;

	private BlockingQueue<Request<ACTION>> requestsQueue = new LinkedBlockingQueue<>();

	public StoreNew(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState) {
		Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

		this.results$ = Flux.create((FluxSink<Request<ACTION>> sink) -> {
			while (true) {
				try {
					Request<ACTION> request = requestsQueue.take();
					sink.next(request);
				} catch (InterruptedException e) {
					sink.error(e);
					return;
				}
			}
		}).subscribeOn(Schedulers.elastic())
				.doOnNext(request -> logger.debug("3: " + request + "\n"))
				.scan(initialResult, (Result<STATE_IMPL, ACTION> lastResult, Request<ACTION> request) -> {
					Result<STATE_IMPL, ACTION> result = reducer.reducer(lastResult.getState(), request);
					if (!result.isNewState()) {
						logger.debug("ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
						return result;
					}
					logger.debug("passed - request: " + request + " new state: " + result.getState() + "\n");
					return result;
				})
				.publish()
				// start emit only when this.states$ listen so he will get the initial state.
				.autoConnect(1);

		this.states$ = this.results$
				.map(Result::getState)
				.distinctUntilChanged()
				.replay(1)
				.autoConnect(0);
	}

	private Mono<Result<STATE_IMPL, ACTION>> dispatch2(Request<ACTION> request) {
		Mono<Result<STATE_IMPL, ACTION>> resultFlux = this.results$
				.filter(result -> result.getRequest().equals(request))
				.take(1)
				.replay(1)
				.autoConnect(0)
				.take(1)
				.single();
		try {
			this.requestsQueue.put(request);
		} catch (InterruptedException e) {
			return Mono.error(e);
		}
		return resultFlux;
	}

	public Mono<STATE_IMPL> dispatch(ACTION action) {
		return dispatch2(new Request<>(action)).map(Result::getState);
	}

	public Mono<Result<STATE_IMPL, ACTION>> dispatch(Request<ACTION> request) {
		return dispatch2(request);
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
