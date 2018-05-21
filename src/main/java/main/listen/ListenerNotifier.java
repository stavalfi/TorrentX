package main.listen;

import main.listen.state.tree.ListenerState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ListenerNotifier {
    Mono<ListenerState> getLatestState$();

    Flux<ListenerState> getState$();

    Flux<ListenerState> getHistory$();

    Flux<ListenerState> getByAction$(ListenerAction action);

    Mono<ListenerState> notifyWhen(ListenerAction when);

    <T> Mono<T> notifyWhen(ListenerAction when, T mapTo);
}
