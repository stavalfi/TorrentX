package main.peer;

import main.TorrentInfo;
import main.peer.peerMessages.*;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.BitSet;

public class PeersCommunicator implements SendPeerMessage {
    private Peer me;
    private Peer peer;
    private Socket peerSocket;
    private TorrentInfo torrentInfo;
    private Flux<PeerMessage> sentMessagesFlux;
    private FluxSink<PeerMessage> sentMessagesFluxSink;
    private DataOutputStream peerDataOutputStream;

    public ReceiveMessages receiveMessages;
    private SpeedStatistics peerSpeedStatistics;

    public PeersCommunicator(TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                             DataInputStream dataInputStream, DataOutputStream peerDataOutputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.torrentInfo = torrentInfo;
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.peerDataOutputStream = peerDataOutputStream;

        this.receiveMessages = new ReceiveMessagesImpl(this.torrentInfo, this.me, this.peer, dataInputStream);
        this.sentMessagesFlux = Flux.create((FluxSink<PeerMessage> sink) -> this.sentMessagesFluxSink = sink);

        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                this.receiveMessages.getPeerMessageResponseFlux(),
                this.sentMessagesFlux);
    }

    public ReceiveMessages receivePeerMessages() {
        return receiveMessages;
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
        if (this.sentMessagesFluxSink != null)
            this.sentMessagesFluxSink.complete();
        try {
            this.peerDataOutputStream.close();
            this.peerSocket.close();
        } catch (IOException exception) {
            // TODO: do something better... it's a fatal problem with my design!!!
            exception.printStackTrace();
        }
    }

    private Mono<PeersCommunicator> send(PeerMessage peerMessage) {
        return Mono.create((MonoSink<PeersCommunicator> monoSink) -> {
            try {
                this.peerDataOutputStream.write(peerMessage.createPacketFromObject());
                monoSink.success(this);
            } catch (IOException e) {
                closeConnection();
                monoSink.error(e);
            }
        }).subscribeOn(Schedulers.elastic())
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty());
    }

    @Override
    public Mono<PeersCommunicator> sendPieceMessage(int index, int begin, byte[] block) {
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

    @Override
    public Flux<PeerMessage> getSentMessagesFlux() {
        return this.sentMessagesFlux;
    }

    public SpeedStatistics getPeerSpeedStatistics() {
        return peerSpeedStatistics;
    }

    @Override
    public String toString() {
        return "PeersCommunicator{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }
}
