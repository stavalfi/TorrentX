package main.peer;


import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class PeersCommunicator {
    private boolean IWantToCloseConnection;
    private final Peer me;
    private final Peer peer;
    private final Socket peerSocket;
    private final Flux<PeerMessage> responses;

    public PeersCommunicator(Peer peer, Socket peerSocket, DataInputStream dataInputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.IWantToCloseConnection = false;
        this.responses = Flux.create((FluxSink<PeerMessage> sink) -> listenForPeerMessages(sink, dataInputStream));
        //.log(null, Level.WARNING, true, SignalType.ON_ERROR);
    }

    public Mono<Void> send(PeerMessage peerMessage) {
        try {
            this.peerSocket.getOutputStream().write(peerMessage.createPacketFromObject());
            return Mono.empty();
        } catch (IOException e) {
            closeConnection();
            return Mono.error(e);
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
        return this.peer.toString();
    }
}
