package main.peer;

import main.TorrentInfo;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;

class ReceiveMessagesNotificationsImpl implements ReceiveMessagesNotifications {
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

    private PeerCurrentStatus peerCurrentStatus;

    public ReceiveMessagesNotificationsImpl(TorrentInfo torrentInfo, Peer me, Peer peer,
                                            PeerCurrentStatus peerCurrentStatus, DataInputStream dataInputStream) {
        this.peerCurrentStatus = peerCurrentStatus;

        this.peerMessageResponseFlux = Flux.generate(synchronousSink -> synchronousSink.next(0))
                .publishOn(Schedulers.elastic())
                .flatMap(__ -> PeerMessageFactory.waitForMessage(torrentInfo, peer, me, dataInputStream))
                //.onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                // there are multiple subscribers to this source (every specific peer-message flux).
                // all of them must get the same message and ***not activate this source more then once***.
                .doOnNext(peerMessage -> {
                    PeerMessageId peerMessageId = PeerMessageId.fromValue(peerMessage.getMessageId());
                    switch (peerMessageId) {
                        case bitFieldMessage:
                            this.peerCurrentStatus.updatePiecesStatus(((BitFieldMessage) peerMessage).getPiecesStatus());
                            break;
                        case haveMessage:
                            this.peerCurrentStatus.updatePiecesStatus(((HaveMessage) peerMessage).getPieceIndex());
                            break;
                        case interestedMessage:
                            this.peerCurrentStatus.setIsHeInterestedInMe(true);
                            break;
                        case notInterestedMessage:
                            this.peerCurrentStatus.setIsHeInterestedInMe(false);
                            break;
                        case chokeMessage:
                            this.peerCurrentStatus.setIsHeChokingMe(true);
                            break;
                        case unchokeMessage:
                            this.peerCurrentStatus.setIsHeChokingMe(false);
                            break;
                    }
                })
                .publish()
                .autoConnect();

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

}
