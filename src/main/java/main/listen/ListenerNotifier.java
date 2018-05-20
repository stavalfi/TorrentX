package main.listen;

import main.listen.state.tree.ListenerState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ListenerNotifier {
    Mono<ListenerState> getLatestState$();

    Flux<ListenerState> getState$();

    Flux<ListenerState> getHistory$();

    Flux<ListenerAction> getAction$();
}
