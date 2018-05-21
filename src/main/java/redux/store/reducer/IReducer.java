package redux.store.reducer;

import redux.store.State;

public interface IReducer<S extends State<A>, A> {

    S reducer(S lastState, A action);
}
