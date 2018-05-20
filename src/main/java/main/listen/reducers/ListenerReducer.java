package main.listen.reducers;

import main.listen.ListenerAction;
import main.listen.state.tree.ListenerState;

import java.util.function.Supplier;

public class ListenerReducer {
    public static Supplier<ListenerState> defaultListenState = () ->
            ListenerState.ListenStateBuilder.builder(ListenerAction.INITIALIZE)
                    .setStartedListeningInProgress(false)
                    .setStartedListeningSelfResolved(false)
                    .setStartedListeningWindUp(false)
                    .setResumeListeningInProgress(false)
                    .setResumeListeningSelfResolved(false)
                    .setResumeListeningWindUp(false)
                    .setPauseListeningInProgress(false)
                    .setPauseListeningSelfResolved(false)
                    .setResumeListeningWindUp(true)
                    .setRestartListeningInProgress(false)
                    .setRestartListeningSelfResolved(false)
                    .setRestartListeningWindUp(false)
                    .build();

    public ListenerState reducer(ListenerState lastState, ListenerAction action) {
        switch (action) {
            case INITIALIZE:
                if (!lastState.isRestartListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setRestartListeningWindUp(false)
                        .build();

            case START_LISTENING_IN_PROGRESS:
                if (lastState.isStartedListeningInProgress() ||
                        lastState.isStartedListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setStartedListeningInProgress(true)
                        .build();

            case START_LISTENING_SELF_RESOLVED:
                if (!lastState.isStartedListeningInProgress() ||
                        lastState.isStartedListeningSelfResolved() ||
                        lastState.isStartedListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setStartedListeningSelfResolved(true)
                        .build();

            case START_LISTENING_WIND_UP:
                if (!lastState.isStartedListeningInProgress() ||
                        !lastState.isStartedListeningSelfResolved() ||
                        lastState.isStartedListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setStartedListeningInProgress(false)
                        .setStartedListeningSelfResolved(false)
                        .setStartedListeningWindUp(true)
                        .build();

            case RESUME_LISTENING_IN_PROGRESS:
                if (lastState.isPauseListeningInProgress() ||
                        !lastState.isStartedListeningWindUp() ||
                        lastState.isRestartListeningInProgress() ||
                        lastState.isResumeListeningInProgress() ||
                        lastState.isResumeListeningWindUp() ||
                        !lastState.isPauseListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setResumeListeningInProgress(true)
                        .build();

            case RESUME_LISTENING_SELF_RESOLVED:
                if (lastState.isPauseListeningInProgress() ||
                        lastState.isRestartListeningInProgress() ||
                        !lastState.isResumeListeningInProgress() ||
                        lastState.isResumeListeningSelfResolved() ||
                        lastState.isResumeListeningWindUp() ||
                        !lastState.isPauseListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setResumeListeningSelfResolved(true)
                        .build();

            case RESUME_LISTENING_WIND_UP:
                if (lastState.isPauseListeningInProgress() ||
                        lastState.isRestartListeningInProgress() ||
                        !lastState.isResumeListeningInProgress() ||
                        !lastState.isResumeListeningSelfResolved() ||
                        lastState.isResumeListeningWindUp() ||
                        !lastState.isPauseListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setResumeListeningInProgress(false)
                        .setResumeListeningSelfResolved(false)
                        .setResumeListeningWindUp(true)
                        .setPauseListeningWindUp(false)
                        .build();

            case PAUSE_LISTENING_IN_PROGRESS:
                if (!lastState.isStartedListeningWindUp() ||
                        lastState.isPauseListeningInProgress() ||
                        lastState.isPauseListeningWindUp() ||
                        !lastState.isResumeListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setPauseListeningInProgress(true)
                        .setResumeListeningInProgress(false)
                        .setResumeListeningSelfResolved(false)
                        .build();

            case PAUSE_LISTENING_SELF_RESOLVED:
                if (!lastState.isPauseListeningInProgress() ||
                        lastState.isPauseListeningSelfResolved() ||
                        lastState.isPauseListeningWindUp() ||
                        !lastState.isResumeListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setPauseListeningSelfResolved(true)
                        .setResumeListeningInProgress(false)
                        .setResumeListeningSelfResolved(false)
                        .build();

            case PAUSE_LISTENING_WIND_UP:
                if (lastState.isRestartListeningInProgress() ||
                        !lastState.isPauseListeningInProgress() ||
                        !lastState.isPauseListeningSelfResolved() ||
                        lastState.isPauseListeningWindUp() ||
                        !lastState.isResumeListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setPauseListeningInProgress(false)
                        .setPauseListeningSelfResolved(false)
                        .setPauseListeningWindUp(true)
                        .build();

            case RESTART_LISTENING_IN_PROGRESS:
                if (lastState.isRestartListeningInProgress() ||
                        lastState.isRestartListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setRestartListeningInProgress(true)
                        .build();

            case RESTART_LISTENING_SELF_RESOLVED:
                if (!lastState.isPauseListeningWindUp() ||
                        !lastState.isRestartListeningInProgress() ||
                        lastState.isRestartListeningSelfResolved() ||
                        lastState.isRestartListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setRestartListeningSelfResolved(true)
                        .build();

            case RESTART_LISTENING_WIND_UP:
                if (!lastState.isRestartListeningInProgress() ||
                        lastState.isRestartListeningSelfResolved() ||
                        lastState.isRestartListeningWindUp())
                    return lastState;
                return ListenerState.ListenStateBuilder.builder(action)
                        .setStartedListeningInProgress(false)
                        .setStartedListeningSelfResolved(false)
                        .setStartedListeningWindUp(false)
                        .setRestartListeningInProgress(false)
                        .setRestartListeningSelfResolved(false)
                        .setRestartListeningWindUp(true)
                        .build();

            default:
                return lastState;
        }
    }
}
