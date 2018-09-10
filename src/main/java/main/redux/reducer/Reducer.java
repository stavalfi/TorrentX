package main.redux.reducer;

import main.redux.state.State;
import main.redux.store.Request;
import main.redux.store.Result;

@FunctionalInterface
public interface Reducer<STATE_IMPL extends State<ACTION>, ACTION> {

	Result<STATE_IMPL, ACTION> reducer(STATE_IMPL lastState, Request<ACTION> request);
}
