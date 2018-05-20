package main.listen;

public class ListenSideEffects {
    public ListenSideEffects(ListenStore store) {
        store.getAction$()
                .filter(ListenAction.START_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenAction.START_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenAction.RESUME_LISTENING_IN_PROGRESS))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenAction.RESUME_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenAction.RESUME_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenAction.PAUSE_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenAction.PAUSE_LISTENING_WIND_UP))
                .publish()
                .autoConnect(0);

        store.getAction$()
                .filter(ListenAction.RESTART_LISTENING_IN_PROGRESS::equals)
                .flatMap(__ -> store.dispatchAsLongNoCancel(ListenAction.RESTART_LISTENING_WIND_UP))
                .flatMap(__ -> store.dispatch(ListenAction.INITIALIZE))
                .publish()
                .autoConnect(0);
    }
}
