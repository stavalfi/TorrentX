package main.listen;

public class ListenerSideEffects {
    public ListenerSideEffects(ListenerStore store) {
        store.getAction$()
                .filter(ListenerAction.START_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.START_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenerAction.RESUME_LISTENING_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenerAction.RESUME_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESUME_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenerAction.PAUSE_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.PAUSE_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenerAction.RESTART_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenerAction.RESTART_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenerAction.INITIALIZE))
                .publish()
                .autoConnect(0);
    }
}
