package main.peer;

import main.peer.peerMessages.*;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.AbstractMap;

public class IncomingPeerMessagesNotifierImpl implements IncomingPeerMessagesNotifier {
    private Flux<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$;
    private Flux<? extends PeerMessage> peerMessageResponseFlux;

    private Flux<BitFieldMessage> bitFieldMessageResponseFlux;
    private Flux<CancelMessage> cancelMessageResponseFlux;
    private Flux<ChokeMessage> chokeMessageResponseFlux;
    private Flux<ExtendedMessage> extendedMessageResponseFlux;
    private Flux<HaveMessage> haveMessageResponseFlux;
    private Flux<InterestedMessage> interestedMessageResponseFlux;
    private Flux<KeepAliveMessage> keepMessageResponseFlux;
    private Flux<NotInterestedMessage> notInterestedMessageResponseFlux;
    private Flux<PieceMessage> pieceMessageResponseFlux;
    private Flux<PortMessage> portMessageResponseFlux;
    private Flux<RequestMessage> requestMessageResponseFlux;
    private Flux<UnchokeMessage> unchokeMessageResponseFlux;

    public IncomingPeerMessagesNotifierImpl(EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$) {
        this.incomingPeerMessages$ = incomingPeerMessages$;
        this.peerMessageResponseFlux = incomingPeerMessages$.map(tuple2 -> tuple2.getValue());

        this.bitFieldMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class);

        this.cancelMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);

        this.chokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class);

        this.extendedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);

        this.haveMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class);

        this.interestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class);

        this.keepMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);

        this.notInterestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class);

        this.pieceMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class);

        this.portMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PortMessage)
                .cast(PortMessage.class);

        this.requestMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class);

        this.unchokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof UnchokeMessage)
                .cast(UnchokeMessage.class);

    }

    @Override
    public Flux<? extends PeerMessage> getPeerMessageResponseFlux() {
        return this.peerMessageResponseFlux;
    }

    @Override
    public Flux<BitFieldMessage> getBitFieldMessageResponseFlux() {
        return bitFieldMessageResponseFlux;
    }

    @Override
    public Flux<CancelMessage> getCancelMessageResponseFlux() {
        return cancelMessageResponseFlux;
    }

    @Override
    public Flux<ChokeMessage> getChokeMessageResponseFlux() {
        return chokeMessageResponseFlux;
    }

    @Override
    public Flux<ExtendedMessage> getExtendedMessageResponseFlux() {
        return extendedMessageResponseFlux;
    }

    @Override
    public Flux<HaveMessage> getHaveMessageResponseFlux() {
        return haveMessageResponseFlux;
    }

    @Override
    public Flux<InterestedMessage> getInterestedMessageResponseFlux() {
        return interestedMessageResponseFlux;
    }

    @Override
    public Flux<KeepAliveMessage> getKeepMessageResponseFlux() {
        return keepMessageResponseFlux;
    }

    @Override
    public Flux<NotInterestedMessage> getNotInterestedMessageResponseFlux() {
        return notInterestedMessageResponseFlux;
    }

    @Override
    public Flux<PieceMessage> getPieceMessageResponseFlux() {
        return pieceMessageResponseFlux;
    }

    @Override
    public Flux<PortMessage> getPortMessageResponseFlux() {
        return portMessageResponseFlux;
    }

    @Override
    public Flux<RequestMessage> getRequestMessageResponseFlux() {
        return requestMessageResponseFlux;
    }

    @Override
    public Flux<UnchokeMessage> getUnchokeMessageResponseFlux() {
        return unchokeMessageResponseFlux;
    }

    @Override
    public Flux<BitFieldMessage> getBitFieldMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class);
    }

    @Override
    public Flux<CancelMessage> getCancelMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);
    }

    @Override
    public Flux<ChokeMessage> getChokeMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class);
    }

    @Override
    public Flux<ExtendedMessage> getExtendedMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);
    }

    @Override
    public Flux<HaveMessage> getHaveMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class);
    }

    @Override
    public Flux<InterestedMessage> getInterestedMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class);
    }

    @Override
    public Flux<KeepAliveMessage> getKeepMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);
    }

    @Override
    public Flux<NotInterestedMessage> getNotInterestedMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class);
    }

    @Override
    public Flux<PieceMessage> getPieceMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class);
    }

    @Override
    public Flux<PortMessage> getPortMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof PortMessage)
                .cast(PortMessage.class);
    }

    @Override
    public Flux<RequestMessage> getRequestMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class);
    }

    @Override
    public Flux<UnchokeMessage> getUnchokeMessageResponse$(Link link) {
        return getIncomingPeerMessages$()
                .filter(peerMessage -> peerMessage.getKey().equals(link))
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(peerMessage -> peerMessage instanceof UnchokeMessage)
                .cast(UnchokeMessage.class);
    }

    @Override
    public Flux<AbstractMap.SimpleEntry<Link, PeerMessage>> getIncomingPeerMessages$() {
        return incomingPeerMessages$;
    }
}
