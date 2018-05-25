package redux.reducer;

import redux.state.State;

@FunctionalInterface
public interface Reducer<S extends State<A>, A> {

    S reducer(S lastState, A action);
}
