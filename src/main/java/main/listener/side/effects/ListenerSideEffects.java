package main.listener.side.effects;

import main.listener.ListenerAction;
import main.listener.state.tree.ListenerState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class ListenerSideEffects {
    private Flux<ListenerState> startListen$;
    private Flux<ListenerState> resumeListen$;
    private Flux<ListenerState> pauseListen$;
    private Flux<ListenerState> restartListen$;

    public ListenerSideEffects(Store<ListenerState, ListenerAction> store) {
        this.startListen$ = store.getByAction$(ListenerAction.START_LISTENING_IN_PROGRESS)
                .switchMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.START_LISTENING_WIND_UP))
                .switchMap(__ -> store.dispatch(ListenerAction.RESUME_LISTENING_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeListen$ = store.getByAction$(ListenerAction.RESUME_LISTENING_IN_PROGRESS)
                .switchMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESUME_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseListen$ = store.getByAction$(ListenerAction.PAUSE_LISTENING_IN_PROGRESS)
                .switchMap(__ -> store.notifyWhen(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED))
                .switchMap(__ -> store.dispatch(ListenerAction.PAUSE_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        this.restartListen$ = store.getByAction$(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                .switchMap(__ -> store.dispatch(ListenerAction.PAUSE_LISTENING_IN_PROGRESS))
                .switchMap(__ -> store.notifyWhen(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
                .switchMap(__ -> store.notifyWhen(ListenerAction.PAUSE_LISTENING_WIND_UP))
                .switchMap(__ -> store.dispatch(ListenerAction.RESTART_LISTENING_WIND_UP))
                .switchMap(__ -> store.dispatch(ListenerAction.INITIALIZE))
                .publish()
                .autoConnect(0);
    }

    public Flux<ListenerState> getStartListen$() {
        return startListen$;
    }

    public Flux<ListenerState> getResumeListen$() {
        return resumeListen$;
    }

    public Flux<ListenerState> getPauseListen$() {
        return pauseListen$;
    }

    public Flux<ListenerState> getRestartListen$() {
        return restartListen$;
    }
}
