package redux.store;

import redux.state.State;

import java.util.Objects;

public class Result<S extends State<A>, A> {
	private Request<A> request;
	private S state;
	private boolean isNewState;

	public Result(Request<A> request, S state, boolean isNewState) {
		this.request = request;
		this.state = state;
		this.isNewState = isNewState;
	}

	public Request<A> getRequest() {
		return request;
	}

	public S getState() {
		return state;
	}

	public boolean isNewState() {
		return isNewState;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result<?, ?> result = (Result<?, ?>) o;
		return isNewState == result.isNewState &&
				Objects.equals(request, result.request) &&
				Objects.equals(state, result.state);
	}

	@Override
	public int hashCode() {

		return Objects.hash(request, state, isNewState);
	}

	@Override
	public String toString() {
		return "Result{" +
				"request=" + request +
				", state=" + state +
				", isNewState=" + isNewState +
				'}';
	}
}
