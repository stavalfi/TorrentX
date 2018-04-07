package main.peer;

import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;

public interface ReceivePeerMessages {
    Flux<? extends PeerMessage> getPeerMessageResponseFlux();

    Flux<BitFieldMessage> getBitFieldMessageResponseFlux();

    Flux<CancelMessage> getCancelMessageResponseFlux();

    Flux<ChokeMessage> getChokeMessageResponseFlux();

    Flux<ExtendedMessage> getExtendedMessageResponseFlux();

    Flux<HaveMessage> getHaveMessageResponseFlux();

    Flux<InterestedMessage> getInterestedMessageResponseFlux();

    Flux<KeepAliveMessage> getKeepMessageResponseFlux();

    Flux<NotInterestedMessage> getNotInterestedMessageResponseFlux();

    Flux<PieceMessage> getPieceMessageResponseFlux();

    Flux<PortMessage> getPortMessageResponseFlux();

    Flux<RequestMessage> getRequestMessageResponseFlux();

    Flux<UnchokeMessage> getUnchokeMessageResponseFlux();
}
