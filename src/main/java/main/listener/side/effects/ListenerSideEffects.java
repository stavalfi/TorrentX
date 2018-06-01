package main.listener.side.effects;

import main.listener.ListenerAction;
import main.listener.reducers.ListenerReducer;
import main.listener.state.tree.ListenerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redux.store.StoreNew;

import java.util.function.BiPredicate;

public class ListenerSideEffects {
	private static Logger logger = LoggerFactory.getLogger(ListenerSideEffects.class);

	private Flux<ListenerState> startListen$;
	private Flux<ListenerState> resumeListen$;
	private Flux<ListenerState> pauseListen$;
	private Flux<ListenerState> restartListen$;

	public ListenerSideEffects(StoreNew<ListenerState, ListenerAction> store) {
		BiPredicate<ListenerAction, ListenerState> isInitialized = (desiredChange, listenerState) ->
				ListenerReducer.defaultListenState.getAction().equals(listenerState.getAction()) &&
						ListenerReducer.defaultListenState.isStartedListeningInProgress() == listenerState.isStartedListeningInProgress() &&
						ListenerReducer.defaultListenState.isStartedListeningSelfResolved() == listenerState.isStartedListeningSelfResolved() &&
						ListenerReducer.defaultListenState.isStartedListeningWindUp() == listenerState.isStartedListeningWindUp() &&
						ListenerReducer.defaultListenState.isResumeListeningInProgress() == listenerState.isResumeListeningInProgress() &&
						ListenerReducer.defaultListenState.isResumeListeningSelfResolved() == listenerState.isResumeListeningSelfResolved() &&
						ListenerReducer.defaultListenState.isResumeListeningWindUp() == listenerState.isResumeListeningWindUp() &&
						ListenerReducer.defaultListenState.isPauseListeningInProgress() == listenerState.isPauseListeningInProgress() &&
						ListenerReducer.defaultListenState.isPauseListeningSelfResolved() == listenerState.isPauseListeningSelfResolved() &&
						ListenerReducer.defaultListenState.isPauseListeningWindUp() == listenerState.isPauseListeningWindUp() &&
						ListenerReducer.defaultListenState.isRestartListeningInProgress() == listenerState.isRestartListeningInProgress() &&
						ListenerReducer.defaultListenState.isRestartListeningSelfResolved() == listenerState.isRestartListeningSelfResolved() &&
						ListenerReducer.defaultListenState.isRestartListeningWindUp() == listenerState.isRestartListeningWindUp();

		BiPredicate<ListenerAction, ListenerState> isRestartedOrRestarting = (desiredChange, listenerState) ->
				listenerState.fromAction(ListenerAction.RESTART_LISTENING_IN_PROGRESS) ||
						listenerState.fromAction(ListenerAction.RESTART_LISTENING_WIND_UP);

		BiPredicate<ListenerAction, ListenerState> isCorrespondingIsProgressCanceled = (desiredChange, listenerState) ->
				!listenerState.fromAction(ListenerAction.getCorrespondingIsProgressAction(desiredChange));

		BiPredicate<ListenerAction, ListenerState> didWeAlreadySucceed = (desiredChange, listenerState) ->
				listenerState.fromAction(desiredChange);

		BiPredicate<ListenerAction, ListenerState> isStartCanceled = isInitialized.or(isRestartedOrRestarting)
				.or(didWeAlreadySucceed).or(isCorrespondingIsProgressCanceled);

		BiPredicate<ListenerAction, ListenerState> isResumeCanceled = isInitialized.or(isRestartedOrRestarting)
				.or(didWeAlreadySucceed).or(isCorrespondingIsProgressCanceled);

		this.startListen$ = store.statesByAction(ListenerAction.START_LISTENING_IN_PROGRESS)
				.concatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.START_LISTENING_WIND_UP, isStartCanceled))
				.doOnNext(__ -> logger.debug("side effects end deal with start and it start resume... 1 :" + __))
				.concatMap(__ -> store.dispatch(ListenerAction.RESUME_LISTENING_IN_PROGRESS))
				.doOnNext(__ -> logger.debug("side effects end deal with start and it start resume... 2 :" + __))
				.publish()
				.autoConnect(0);

		this.resumeListen$ = store.statesByAction(ListenerAction.RESUME_LISTENING_IN_PROGRESS)
				.doOnNext(__ -> logger.debug("side effects start deal with resume... 1 :" + __))
				.concatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESUME_LISTENING_WIND_UP, isResumeCanceled))
				.publish()
				.autoConnect(0);

		this.pauseListen$ = store.statesByAction(ListenerAction.PAUSE_LISTENING_IN_PROGRESS)
				.concatMap(__ -> store.notifyWhen(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED))
				.concatMap(__ -> store.dispatch(ListenerAction.PAUSE_LISTENING_WIND_UP))
				.publish()
				.autoConnect(0);

		this.restartListen$ = store.statesByAction(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
				.doOnNext(__ -> logger.debug("8..."))
				.concatMap(__ -> store.dispatch(ListenerAction.PAUSE_LISTENING_IN_PROGRESS))
				.doOnNext(__ -> logger.debug("9..."))
				.concatMap(__ -> store.notifyWhen(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
				.doOnNext(__ -> logger.debug("10..."))
				.concatMap(__ -> store.notifyWhen(ListenerAction.PAUSE_LISTENING_WIND_UP))
				.doOnNext(__ -> logger.debug("11..."))
				.concatMap(__ -> store.dispatch(ListenerAction.RESTART_LISTENING_WIND_UP))
				.doOnNext(__ -> logger.debug("12..."))
				.concatMap(__ -> store.dispatch(ListenerAction.INITIALIZE))
				.doOnNext(__ -> logger.debug("13..."))
				.publish()
				.autoConnect(0);
	}

	public Flux<ListenerState> getStartListen$() {
		return startListen$.publishOn(Schedulers.elastic());
	}

	public Flux<ListenerState> getResumeListen$() {
		return resumeListen$.publishOn(Schedulers.elastic());
	}

	public Flux<ListenerState> getPauseListen$() {
		return pauseListen$.publishOn(Schedulers.elastic());
	}

	public Flux<ListenerState> getRestartListen$() {
		return restartListen$.publishOn(Schedulers.elastic());
	}
}
