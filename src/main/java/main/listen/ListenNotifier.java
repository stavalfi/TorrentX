package main.listen;

import main.listen.state.tree.ListenState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ListenNotifier {
    Mono<ListenState> getLatestState$();

    Flux<ListenState> getState$();

    Flux<ListenState> getHistory$();

    Flux<ListenAction> getAction$();
}
