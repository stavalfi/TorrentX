package redux.reducer;

import redux.state.State;
import redux.store.StoreNew.Request;

@FunctionalInterface
public interface Reducer<STATE_IMPL extends State<ACTION>, ACTION> {

    STATE_IMPL reducer(STATE_IMPL lastState, Request<ACTION> action);
}
