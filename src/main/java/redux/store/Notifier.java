package redux.store;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.state.State;

public interface Notifier<S extends State<A>, A> {
    Mono<S> latestState$();

    Flux<S> states$();

    Flux<S> statesHistory();

    Flux<S> statesByAction(A action);

    Mono<S> notifyWhen(A when);

    <U> Mono<U> notifyWhen(A when, U mapTo);
}
