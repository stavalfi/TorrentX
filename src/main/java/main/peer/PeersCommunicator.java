package main.peer;

import main.TorrentInfo;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.BitSet;

public class PeersCommunicator implements SendPeerMessage, ReceivePeerMessages {
    private Peer me;
    private Peer peer;
    private BitSet peerPieces;
    private Socket peerSocket;
    private boolean isChoked;
    private boolean isInterestedInMe;
    private TorrentInfo torrentInfo;
    private Flux<PeerMessage> peerMessageFlux;
    private Flux<Double> peerDownloadSpeedFlux;
    private Flux<Double> peerUploadSpeedFlux;
    private FluxSink<PieceMessage> outGoingPiecesFluxSink;

    // receive messages:

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

    private boolean IWantToCloseConnection;

    public PeersCommunicator(TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                             DataInputStream dataInputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.isChoked = false;
        this.isInterestedInMe = false;
        this.peerSocket = peerSocket;
        this.torrentInfo = torrentInfo;
        this.peerPieces = new BitSet(torrentInfo.getPieces().size());
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.IWantToCloseConnection = false;

        Flux<PeerMessage> peerMessageResponseFlux = Flux.create((FluxSink<PeerMessage> sink) -> listenForPeerMessages(sink, dataInputStream))
                .subscribeOn(Schedulers.elastic())
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty());

        this.bitFieldMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class)
                .doOnNext(bitFieldMessage -> this.peerPieces.or(bitFieldMessage.getPieces()));

        this.cancelMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);

        this.chokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class)
                .doOnNext(chokeMessage -> this.isChoked = true);

        this.extendedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);

        this.haveMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class)
                .doOnNext(haveMessage -> this.peerPieces.set(haveMessage.getPieceIndex()));

        this.interestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class)
                .doOnNext(interestedMessage -> this.isInterestedInMe = true);

        this.keepMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);

        this.notInterestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class)
                .doOnNext(notInterestedMessage -> this.isInterestedInMe = false);

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
                .cast(UnchokeMessage.class)
                .doOnNext(unchokeMessage -> this.isChoked = false);

        this.peerMessageFlux = Flux.merge(this.bitFieldMessageResponseFlux, this.cancelMessageResponseFlux, this.chokeMessageResponseFlux,
                this.extendedMessageResponseFlux, this.haveMessageResponseFlux, this.interestedMessageResponseFlux,
                this.keepMessageResponseFlux, this.notInterestedMessageResponseFlux, this.pieceMessageResponseFlux,
                this.portMessageResponseFlux, this.requestMessageResponseFlux, this.unchokeMessageResponseFlux);

        this.peerDownloadSpeedFlux = this.pieceMessageResponseFlux
                .map(PeerMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);

        this.peerUploadSpeedFlux = Flux.create((FluxSink<PieceMessage> sink) -> this.outGoingPiecesFluxSink = sink)
                .map(PieceMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);
    }

    private void listenForPeerMessages(FluxSink<PeerMessage> sink, DataInputStream dataInputStream) {
        while (!sink.isCancelled() && !this.peerSocket.isClosed() && this.peerSocket.isConnected()) {
            try {
                PeerMessage peerMessage = PeerMessageFactory.create(this.peer, this.me, dataInputStream);
                sink.next(peerMessage);
            } catch (IOException e) {
                if (!this.IWantToCloseConnection) // only if it wasn't because of me.
                    sink.error(e);
                try {
                    dataInputStream.close();
                    closeConnection();
                } catch (IOException e1) {
                    // TODO: do something better... it's a fatal problem with my design!!!
                    e1.printStackTrace();
                }
            }
        }
    }

    private boolean doesPeerHavePiece(int pieceIndex) {
        return this.peerPieces.get(pieceIndex);
    }

    /**
     * @return an hot flux!
     */
    public Flux<PeerMessage> receive() {
        return this.peerMessageFlux;
    }

    public Peer getPeer() {
        return peer;
    }

    public Peer getMe() {
        return me;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public void closeConnection() {
        this.IWantToCloseConnection = true;
        try {
            this.peerSocket.close();
        } catch (IOException exception) {
            // TODO: do something better... it's a fatal problem with my design!!!
            exception.printStackTrace();
        }
    }

    private Mono<Void> send(PeerMessage peerMessage) {
        try {
            this.peerSocket.getOutputStream().write(peerMessage.createPacketFromObject());
            return Mono.empty();
        } catch (IOException e) {
            closeConnection();
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> sendPieceMessage(int index, int begin, byte[] block) {
        PieceMessage pieceMessage = new PieceMessage(this.getMe(), this.getPeer(), index, begin, block);
        return send(pieceMessage)
                // for calculating the peer upload speed -
                // I do not care if we failed to send the piece.
                // so I don't register to doOnError or something like that.
                .doOnNext(aVoid -> this.outGoingPiecesFluxSink.next(pieceMessage));
    }

    @Override
    public Mono<Void> sendBitFieldMessage(BitSet peaces) {
        return send(new BitFieldMessage(this.getMe(), this.getPeer(), peaces));
    }

    @Override
    public Mono<Void> sendCancelMessage(int index, int begin, int length) {
        return send(new CancelMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<Void> sendChokeMessage() {
        return send(new ChokeMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<Void> sendHaveMessage(int pieceIndex) {
        return send(new HaveMessage(this.getMe(), this.getPeer(), pieceIndex));
    }

    @Override
    public Mono<Void> sendInterestedMessage() {
        return send(new InterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<Void> sendKeepAliveMessage() {
        return send(new KeepAliveMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<Void> sendNotInterestedMessage() {
        return send(new NotInterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<Void> sendPortMessage(short listenPort) {
        return send(new PortMessage(this.getMe(), this.getPeer(), listenPort));
    }

    @Override
    public Mono<Void> sendRequestMessage(int index, int begin, int length) {
        return send(new RequestMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<Void> sendUnchokeMessage() {
        return send(new UnchokeMessage(this.getMe(), this.getPeer()));
    }

    public Flux<Double> getPeerDownloadSpeedFlux() {
        return peerDownloadSpeedFlux;
    }

    public Flux<Double> getPeerUploadSpeedFlux() {
        return peerUploadSpeedFlux;
    }

    public boolean isInterestedInMe() {
        return isInterestedInMe;
    }

    public boolean isChoked() {
        return isChoked;
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
    public String toString() {
        return "PeersCommunicator{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }
}
