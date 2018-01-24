package main.peer;


import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class PeersCommunicator {
    private boolean IWantToCloseConnection;
    private final Peer me;
    private final Peer peer;
    private final Socket peerSocket;
    private final Flux<PeerMessage> responses;

    public PeersCommunicator(Peer peer, Socket peerSocket) throws IOException {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.responses = waitForResponses(peerSocket.getInputStream());
        this.IWantToCloseConnection = false;
    }

    private Flux<PeerMessage> waitForResponses(InputStream inputStream) {
        return Flux.create((FluxSink<PeerMessage> sink) -> {
            Thread thread = new Thread(() -> listenForPeerMessages(sink, inputStream));
            sink.onDispose(thread::interrupt);
            thread.start();
        })
                .log()
                //.log(null, Level.WARNING, true, SignalType.ON_ERROR)
                .publishOn(Schedulers.single());
    }

    public Flux<PeerMessage> send(PeerMessage peerMessage) {
        try {
            this.peerSocket.getOutputStream().write(peerMessage.createPacketFromObject());
            return receive();
        } catch (IOException e) {
            closeConnection();
            return Flux.error(e);
        }
    }

    private void listenForPeerMessages(FluxSink<PeerMessage> sink, InputStream inputStream) {
        while (!sink.isCancelled() && !this.peerSocket.isClosed() && this.peerSocket.isConnected()) {
            try {
                PeerMessage peerMessage = PeerMessageFactory.create(this.me, this.peer, inputStream);
                sink.next(peerMessage);
            } catch (IOException e) {
                if (!this.IWantToCloseConnection)//only if it wasn't because of me.
                    sink.error(e);
                try {
                    inputStream.close();
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
            exception.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return this.peer.toString();
    }
}
