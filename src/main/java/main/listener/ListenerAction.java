package main.listener;

public enum ListenerAction {
    // remove RESTART_LISTENING_WIND_UP flag and all other flags
    INITIALIZE,

    START_LISTENING_IN_PROGRESS,
    START_LISTENING_SELF_RESOLVED,
    START_LISTENING_WIND_UP,

    PAUSE_LISTENING_IN_PROGRESS,
    PAUSE_LISTENING_SELF_RESOLVED,
    PAUSE_LISTENING_WIND_UP,

    RESUME_LISTENING_IN_PROGRESS,
    RESUME_LISTENING_SELF_RESOLVED,
    RESUME_LISTENING_WIND_UP,

    RESTART_LISTENING_IN_PROGRESS,
    RESTART_LISTENING_SELF_RESOLVED,
    // remove all flags but this flag: RESTART_LISTENING_WIND_UP
    RESTART_LISTENING_WIND_UP,;

    public static ListenerAction getCorrespondingIsProgressAction(ListenerAction action) {
        switch (action) {
            case START_LISTENING_WIND_UP:
                return START_LISTENING_IN_PROGRESS;
            case RESUME_LISTENING_WIND_UP:
                return RESUME_LISTENING_IN_PROGRESS;
            case PAUSE_LISTENING_WIND_UP:
                return PAUSE_LISTENING_IN_PROGRESS;
            case RESTART_LISTENING_WIND_UP:
                return RESTART_LISTENING_IN_PROGRESS;
        }
        return null;
    }
}
