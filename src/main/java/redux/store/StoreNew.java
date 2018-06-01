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

	private FluxSink<Request<ACTION>> emitRequestsSink;
	private BlockingQueue<Request<ACTION>> requestsQueue = new LinkedBlockingQueue<>();

	public StoreNew(Reducer<STATE_IMPL, ACTION> reducer, STATE_IMPL defaultState) {
		Result<STATE_IMPL, ACTION> initialResult = new Result<>(new Request<>(defaultState.getAction()), defaultState, true);

		EmitterProcessor<Request<ACTION>> emitRequests = EmitterProcessor.create(Integer.MAX_VALUE);
		this.emitRequestsSink = emitRequests.sink(FluxSink.OverflowStrategy.BUFFER);

		this.results$ = emitRequests
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
				.replay()
				.autoConnect(0);

		this.states$ = this.results$
				.doOnNext(__ -> System.out.println("is new state?: " + __))
				.map(Result::getState)
				.distinctUntilChanged()
				.doOnNext(__ -> System.out.println("new state1: " + __))
				.replay(1)
				.autoConnect(0)
				.doOnNext(__ -> System.out.println("new state2: " + __));
	}

	private Mono<Result<STATE_IMPL, ACTION>> dispatch2(ACTION action) {
		Request<ACTION> request = new Request<>(action);
		logger.debug("getting ready for dispatching request: " + request);
		Flux<Result<STATE_IMPL, ACTION>> resultFlux = this.results$.doOnNext(__ -> System.out.println("received result 1: " + __))
				.doOnNext(__ -> System.out.println("received result 2: " + __))
				.filter(result -> result.getRequest().equals(request))
				.doOnNext(__ -> System.out.println("received result 3: " + __))
				.take(1)
				.doOnNext(__ -> System.out.println("received result 4: " + __))
				.replay()
				.autoConnect(0, disposable -> System.out.println("canceled!"))
				.doOnNext(__ -> System.out.println("received result 5: " + __));
		this.emitRequestsSink.next(request);
		return resultFlux
				.doOnNext(__ -> System.out.println("received result 6: " + __))
				.take(1)
				.doOnNext(__ -> System.out.println("received result 7: " + __))
				.single()
				.doOnNext(__ -> System.out.println("received result 8: " + __))
				.doOnNext(result -> logger.debug("finish process request: " + request));
	}

	public Mono<STATE_IMPL> dispatch(ACTION action) {
		return dispatch2(action).map(Result::getState);
	}

	public Mono<STATE_IMPL> dispatchAsLongNoCancel(ACTION action, BiPredicate<ACTION, STATE_IMPL> isCanceled) {
		return states$()
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
		return this.states$.doOnNext(stateImpl -> logger.debug("latestState$ :" + stateImpl))
				.take(1)
				.single();
	}

	@Override
	public Flux<STATE_IMPL> states$() {
		return this.states$.doOnNext(stateImpl -> logger.debug("states$ :" + stateImpl))
				.publishOn(Schedulers.elastic());
	}

	@Override
	public Flux<STATE_IMPL> statesHistory() {
		return null;
	}

	@Override
	public Flux<STATE_IMPL> statesByAction(ACTION action) {
		return states$().doOnNext(stateImpl -> logger.debug("statesByAction :" + action + " - " + stateImpl))
				.filter(stateImpl -> stateImpl.getAction().equals(action));
	}

	@Override
	public Mono<STATE_IMPL> notifyWhen(ACTION when) {
		System.out.println("notifyWhen 1 - " + when);
		return states$()
				.doOnNext(__ -> System.out.println("notifyWhen 2 - " + when + ": " + __))
				.doOnNext(__ -> System.out.println("notifyWhen 3 - " + when + ": " + __))
				.filter(stateImpl -> stateImpl.fromAction(when))
				.doOnNext(__ -> System.out.println("notifyWhen 4 - " + when + ": " + __))
				.take(1)
				.doOnNext(__ -> System.out.println("notifyWhen 5 - " + when + ": " + __))
				.single();
	}

	@Override
	public <T> Mono<T> notifyWhen(ACTION when, T mapTo) {
		System.out.println("notifyWhenMap 1 - " + when);
		return states$().doOnNext(__ -> System.out.println("notifyWhenMap 2 - " + when + ": " + __))
				.filter(stateImpl -> stateImpl.fromAction(when))
				.doOnNext(__ -> System.out.println("notifyWhenMap 3 - " + when + ": " + __))
				.take(1)
				.doOnNext(__ -> System.out.println("notifyWhenMap 4 - " + when + ": " + __))
				.single()
				.doOnNext(__ -> System.out.println("notifyWhenMap 5 - " + when + ": " + __))
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

//Flux.create((FluxSink<Request<ACTION>> sink) -> {
//		while (true) {
//		try {
//		Request<ACTION> request = requestsQueue.take();
//		sink.next(request);
//		} catch (InterruptedException e) {
//		sink.error(e);
//		return;
//		}
//		}
//		})