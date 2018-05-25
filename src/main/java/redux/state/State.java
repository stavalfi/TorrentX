package redux.state;

import java.util.Objects;

public abstract class State<A> {
    private String id;
    private A action;

    public State(String id, A action) {
        this.id = id == null ? "INITIALIZE-ID" : id;
        this.action = action;
    }

    public abstract boolean fromAction(A action);

    public String getId() {
        return id;
    }

    public A getAction() {
        return action;
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
        return "action=" + action +
                ", id='" + id + '\'' + ",";
    }
}
