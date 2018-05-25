package redux.reducer;

import redux.state.State;
import redux.store.StoreNew.Request;

@FunctionalInterface
public interface Reducer<S extends State<A>, A> {

    S reducer(S lastState, Request<A> action);
}
