package main.peer;

import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;

import java.util.AbstractMap;

public interface IncomingPeerMessagesNotifier {
    Flux<AbstractMap.SimpleEntry<Link, PeerMessage>> getIncomingPeerMessages$();

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

    Flux<BitFieldMessage> getBitFieldMessageResponse$(Link link);

    Flux<CancelMessage> getCancelMessageResponse$(Link link);

    Flux<ChokeMessage> getChokeMessageResponse$(Link link);

    Flux<ExtendedMessage> getExtendedMessageResponse$(Link link);

    Flux<HaveMessage> getHaveMessageResponse$(Link link);

    Flux<InterestedMessage> getInterestedMessageResponse$(Link link);

    Flux<KeepAliveMessage> getKeepMessageResponse$(Link link);

    Flux<NotInterestedMessage> getNotInterestedMessageResponse$(Link link);

    Flux<PieceMessage> getPieceMessageResponse$(Link link);

    Flux<PortMessage> getPortMessageResponse$(Link link);

    Flux<RequestMessage> getRequestMessageResponse$(Link link);

    Flux<UnchokeMessage> getUnchokeMessageResponse$(Link link);
}
