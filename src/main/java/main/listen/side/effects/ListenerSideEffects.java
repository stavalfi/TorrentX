package main.listen.side.effects;

import main.listen.ListenerAction;
import main.listen.ListenerStore;
import main.listen.state.tree.ListenerState;
import reactor.core.publisher.Flux;

public class ListenerSideEffects {
    Flux<ListenerState> startListen$;
    Flux<ListenerState> resumeListen$;
    Flux<ListenerState> pauseListen$;
    Flux<ListenerState> restartListen$;

    public ListenerSideEffects(ListenerStore store) {
        this.startListen$ = store.getByAction$(ListenerAction.START_LISTENING_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.START_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenerAction.RESUME_LISTENING_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        this.resumeListen$ = store.getByAction$(ListenerAction.RESUME_LISTENING_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESUME_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        this.pauseListen$ = store.getByAction$(ListenerAction.PAUSE_LISTENING_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.PAUSE_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        this.restartListen$ = store.getByAction$(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESTART_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenerAction.INITIALIZE))
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
