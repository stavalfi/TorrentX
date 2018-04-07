package main.peer;

import main.App;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

class SendPeerMessagesImpl implements SendPeerMessages {
    private Peer me;
    private Peer peer;
    private DataOutputStream peerDataOutputStream;
    private Flux<PeerMessage> sentPeerMessagesFlux;
    private FluxSink<PeerMessage> sentMessagesFluxSink;
    private Runnable closeConnectionMethod;

    SendPeerMessagesImpl(Peer me, Peer peer,
                         Runnable closeConnectionMethod,
                         DataOutputStream peerDataOutputStream) {
        this.me = me;
        this.peer = peer;
        this.closeConnectionMethod = closeConnectionMethod;
        this.peerDataOutputStream = peerDataOutputStream;
        this.sentPeerMessagesFlux = Flux.create((FluxSink<PeerMessage> sink) -> this.sentMessagesFluxSink = sink);
    }

    private Mono<SendPeerMessages> send(PeerMessage peerMessage) {
        return Mono.create((MonoSink<SendPeerMessages> monoSink) -> {
            try {
                this.peerDataOutputStream.write(peerMessage.createPacketFromObject());
                monoSink.success(this);
            } catch (IOException e) {
                this.closeConnectionMethod.run();
                monoSink.error(e);
            }
        }).subscribeOn(App.MyScheduler)
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty());
    }

    @Override
    public Mono<SendPeerMessages> sendPieceMessage(int index, int begin, byte[] block) {
        PieceMessage pieceMessage = new PieceMessage(this.getMe(), this.getPeer(), index, begin, block);
        return send(pieceMessage)
                // for calculating the peer upload speed -
                // I do not care if we failed to send the piece.
                // so I don't register to doOnError or something like that.
                .doOnNext(peersCommunicator -> {
                    if (this.sentMessagesFluxSink != null)
                        this.sentMessagesFluxSink.next(pieceMessage);
                });
    }

    @Override
    public Mono<SendPeerMessages> sendBitFieldMessage(BitSet peaces) {
        return send(new BitFieldMessage(this.getMe(), this.getPeer(), peaces));
    }

    @Override
    public Mono<SendPeerMessages> sendBitFieldMessage(BitFieldMessage bitFieldMessage) {
        return sendBitFieldMessage(bitFieldMessage.getPieces());
    }

    @Override
    public Mono<SendPeerMessages> sendCancelMessage(int index, int begin, int length) {
        return send(new CancelMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<SendPeerMessages> sendChokeMessage() {
        return send(new ChokeMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendPeerMessages> sendHaveMessage(int pieceIndex) {
        return send(new HaveMessage(this.getMe(), this.getPeer(), pieceIndex));
    }

    @Override
    public Mono<SendPeerMessages> sendInterestedMessage() {
        return send(new InterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendPeerMessages> sendKeepAliveMessage() {
        return send(new KeepAliveMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendPeerMessages> sendNotInterestedMessage() {
        return send(new NotInterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendPeerMessages> sendPortMessage(short listenPort) {
        return send(new PortMessage(this.getMe(), this.getPeer(), listenPort));
    }

    @Override
    public Mono<SendPeerMessages> sendRequestMessage(int index, int begin, int length) {
        return send(new RequestMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<SendPeerMessages> sendUnchokeMessage() {
        return send(new UnchokeMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Flux<PeerMessage> sentPeerMessagesFlux() {
        return this.sentPeerMessagesFlux;
    }

    private Peer getPeer() {
        return peer;
    }

    private Peer getMe() {
        return me;
    }
}
