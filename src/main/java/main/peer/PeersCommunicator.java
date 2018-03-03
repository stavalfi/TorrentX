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
    private boolean peerChokesMe;
    private boolean peerInterestedInMe;
    private TorrentInfo torrentInfo;
    private Flux<Double> peerDownloadSpeedFlux;
    private Flux<Double> peerUploadSpeedFlux;
    private FluxSink<PieceMessage> outGoingPiecesFluxSink;

    // receive messages:

    private Flux<PeerMessage> peerMessageResponseFlux;

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
        this.peerChokesMe = false;
        this.peerInterestedInMe = false;
        this.peerSocket = peerSocket;
        this.torrentInfo = torrentInfo;
        this.peerPieces = new BitSet(torrentInfo.getPieces().size());
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.IWantToCloseConnection = false;

        this.peerMessageResponseFlux =
                Flux.create((FluxSink<PeerMessage> sink) -> listenForPeerMessages(sink, dataInputStream))
                        // it is important to publish from source on different thread then the
                        // subscription to this source's thread every time because:
                        // if not and we subscribe to this specific source multiple times then only the
                        // first subscription will be activated and the source will never end
                        .subscribeOn(Schedulers.elastic())
                        .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                        // there are multiple subscribers to this source (every specific peer-message flux).
                        // all of them must get the same message and not activate this source more then once.
                        .publish()
                        // **any** subscriber to **this** source may start the source to produce signals
                        // to him and everyone else.
                        .autoConnect(1);
        ;

        this.bitFieldMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class)
                .doOnNext(bitFieldMessage -> this.peerPieces.or(bitFieldMessage.getPieces()));

        this.cancelMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);

        this.chokeMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class)
                .doOnNext(chokeMessage -> this.peerChokesMe = true);

        this.extendedMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);

        this.haveMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class)
                .doOnNext(haveMessage -> this.peerPieces.set(haveMessage.getPieceIndex()));

        this.interestedMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class)
                .doOnNext(interestedMessage -> this.peerInterestedInMe = true);

        this.keepMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);

        this.notInterestedMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class)
                .doOnNext(notInterestedMessage -> this.peerInterestedInMe = false);

        this.pieceMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class);

        this.portMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PortMessage)
                .cast(PortMessage.class);

        this.requestMessageResponseFlux = this.peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class);

        this.unchokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof UnchokeMessage)
                .cast(UnchokeMessage.class)
                .doOnNext(unchokeMessage -> this.peerChokesMe = false);

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

    private Mono<PeersCommunicator> send(PeerMessage peerMessage) {
        try {
            this.peerSocket.getOutputStream().write(peerMessage.createPacketFromObject());
            return Mono.just(this);
        } catch (IOException e) {
            closeConnection();
            return Mono.error(e);
        }
    }

    @Override
    public Mono<PeersCommunicator> sendPieceMessage(int index, int begin, byte[] block) {
        PieceMessage pieceMessage = new PieceMessage(this.getMe(), this.getPeer(), index, begin, block);
        return send(pieceMessage)
                // for calculating the peer upload speed -
                // I do not care if we failed to send the piece.
                // so I don't register to doOnError or something like that.
                .doOnNext(peersCommunicator -> {
                    if (this.outGoingPiecesFluxSink != null)
                        this.outGoingPiecesFluxSink.next(pieceMessage);
                });
    }

    @Override
    public Mono<PeersCommunicator> sendBitFieldMessage(BitSet peaces) {
        return send(new BitFieldMessage(this.getMe(), this.getPeer(), peaces));
    }

    @Override
    public Mono<PeersCommunicator> sendCancelMessage(int index, int begin, int length) {
        return send(new CancelMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<PeersCommunicator> sendChokeMessage() {
        return send(new ChokeMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<PeersCommunicator> sendHaveMessage(int pieceIndex) {
        return send(new HaveMessage(this.getMe(), this.getPeer(), pieceIndex));
    }

    @Override
    public Mono<PeersCommunicator> sendInterestedMessage() {
        return send(new InterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<PeersCommunicator> sendKeepAliveMessage() {
        return send(new KeepAliveMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<PeersCommunicator> sendNotInterestedMessage() {
        return send(new NotInterestedMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<PeersCommunicator> sendPortMessage(short listenPort) {
        return send(new PortMessage(this.getMe(), this.getPeer(), listenPort));
    }

    @Override
    public Mono<PeersCommunicator> sendRequestMessage(int index, int begin, int length) {
        return send(new RequestMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<PeersCommunicator> sendUnchokeMessage() {
        return send(new UnchokeMessage(this.getMe(), this.getPeer()));
    }

    public Flux<Double> getPeerDownloadSpeedFlux() {
        return peerDownloadSpeedFlux;
    }

    public Flux<Double> getPeerUploadSpeedFlux() {
        return peerUploadSpeedFlux;
    }

    public boolean isPeerInterestedInMe() {
        return peerInterestedInMe;
    }

    public boolean isPeerChokesMe() {
        return peerChokesMe;
    }

    @Override
    public Flux<PeerMessage> getPeerMessageResponseFlux() {
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
    public String toString() {
        return "PeersCommunicator{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }
}
