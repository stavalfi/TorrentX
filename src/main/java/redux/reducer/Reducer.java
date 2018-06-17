package redux.reducer;

import redux.state.State;
import redux.store.Request;
import redux.store.Result;

@FunctionalInterface
public interface Reducer<STATE_IMPL extends State<ACTION>, ACTION> {

	Result<STATE_IMPL, ACTION> reducer(STATE_IMPL lastState, Request<ACTION> request);
}
