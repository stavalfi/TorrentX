package redux.store;

import redux.state.State;

import java.util.Objects;
import java.util.UUID;

public class RequestForChange<A> {
    private A action;
    private String id;

    public RequestForChange(A action) {
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
        return "RequestForChange{" +
                "action=" + action +
                ", id='" + id + '\'' +
                '}';
    }
}
