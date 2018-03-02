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

public class PeersCommunicator implements SendPeerMessage {
    private Peer me;
    private Peer peer;
    private BitSet peerPieces;
    private Socket peerSocket;
    private boolean isChoked;
    private boolean isInterestedInMe;
    private TorrentInfo torrentInfo;
    private Flux<PeerMessage> responses;
    private Flux<Double> peerDownloadSpeedFlux;
    private Flux<Double> peerUploadSpeedFlux;
    private FluxSink<PieceMessage> outGoingPiecesFluxSink;

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
        this.responses = Flux.create((FluxSink<PeerMessage> sink) -> listenForPeerMessages(sink, dataInputStream))
                .subscribeOn(Schedulers.elastic())
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                .publishOn(Schedulers.single())
                .doOnNext(peerMessage -> proccessPeerMessage(peerMessage))
                .subscribeOn(Schedulers.elastic());

        this.peerDownloadSpeedFlux = this.responses
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .map(PeerMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);

        this.peerUploadSpeedFlux = Flux.create((FluxSink<PieceMessage> sink) -> this.outGoingPiecesFluxSink = sink)
                .map(PieceMessage::getPayload)
                .map((byte[] payload) -> payload.length)
                .map(Double::new);

    }

    /**
     * check if this message is relevant for this class.
     * <p>
     * this method is synchronized because only one
     * thread can be here at any moment.
     *
     * @param peerMessage
     */
    private void proccessPeerMessage(PeerMessage peerMessage) {
        if (peerMessage instanceof BitFieldMessage) {
            BitFieldMessage bitFieldMessage = (BitFieldMessage) peerMessage;
            this.peerPieces.or(bitFieldMessage.getPieces());
            return;
        }
        if (peerMessage instanceof HaveMessage) {
            HaveMessage haveMessage = (HaveMessage) peerMessage;

            this.peerPieces.set(haveMessage.getPieceIndex());
            return;
        }
        if (peerMessage instanceof ChokeMessage) {
            this.isChoked = true;
            return;
        }
        if (peerMessage instanceof UnchokeMessage) {
            this.isChoked = false;
            return;
        }
        if (peerMessage instanceof InterestedMessage) {
            this.isInterestedInMe = true;
            return;
        }
        if (peerMessage instanceof NotInterestedMessage) {
            this.isInterestedInMe = false;
            return;
        }
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
        return this.responses;
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

    @Override
    public String toString() {
        return "PeersCommunicator{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }

    private Mono<Void> send(PeerMessage peerMessage) {
        try {
            if (peerMessage.getMessageId() == 3) {
                System.out.println(peerMessage);
            }
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
}
