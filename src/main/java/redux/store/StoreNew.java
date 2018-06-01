package redux.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redux.reducer.Reducer;
import redux.state.State;

import java.util.Objects;
import java.util.UUID;
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
					STATE_IMPL newState = reducer.reducer(lastResult.getState(), request);
					if (newState.equals(lastResult.getState())) {
						logger.debug("ignored -  request: " + request + " last state: " + lastResult.getState() + "\n");
						return new Result<>(request, lastResult.getState(), false);
					}
					logger.debug("passed - request: " + request + " new state: " + newState + "\n");
					return new Result<>(request, newState, true);
				})
				.replay(1)
				.autoConnect(0);

		this.states$ = this.results$
				.map(Result::getState)
				.distinctUntilChanged()
				.replay(1)
				.autoConnect(0);
	}

	private Mono<Result<STATE_IMPL, ACTION>> dispatch2(ACTION action) {
		Request<ACTION> request = new Request<>(action);
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
		return dispatch2(action).map(Result::getState);
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

	public static class Result<S extends State<A>, A> {
		private Request<A> request;
		private S state;
		private boolean isNewState;

		public Result(Request<A> request, S state, boolean isNewState) {
			this.request = request;
			this.state = state;
			this.isNewState = isNewState;
		}

		public Request<A> getRequest() {
			return request;
		}

		public S getState() {
			return state;
		}

		public boolean isNewState() {
			return isNewState;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Result<?, ?> result = (Result<?, ?>) o;
			return isNewState == result.isNewState &&
					Objects.equals(request, result.request) &&
					Objects.equals(state, result.state);
		}

		@Override
		public int hashCode() {

			return Objects.hash(request, state, isNewState);
		}

		@Override
		public String toString() {
			return "Result{" +
					"request=" + request +
					", state=" + state +
					", isNewState=" + isNewState +
					'}';
		}
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