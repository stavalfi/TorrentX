package redux.store;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Notifier<S extends State<A>, A> {
    Mono<S> getLatestState$();

    Flux<S> getState$();

    Flux<S> getHistory$();

    Flux<S> getByAction$(A action);

    Mono<S> notifyWhen(A when);

    <U> Mono<U> notifyWhen(A when, U mapTo);
}
