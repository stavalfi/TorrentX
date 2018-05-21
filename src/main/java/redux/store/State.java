package redux.store;

import java.util.Objects;

public abstract class State<A> {
    private A action;

    public State(A action) {
        this.action = action;
    }

    public abstract boolean fromAction(A action);

    public A getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State<?> state = (State<?>) o;
        return Objects.equals(getAction(), state.getAction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAction());
    }

    @Override
    public String toString() {
        return "State{" +
                "action=" + action +
                '}';
    }
}
