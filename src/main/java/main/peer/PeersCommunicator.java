package main.peer;


import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PeersCommunicator {
    private final Peer me;
    private final Peer peer;
    private final Socket peerSocket;
    private final DataOutputStream dataOutputStream;
    private final Flux<PeerMessage> responses;

    public PeersCommunicator(Peer peer, Socket peerSocket, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.me = new Peer("localhost", peerSocket.getLocalPort());
        this.dataOutputStream = dataOutputStream;
        this.responses = waitForResponses(dataInputStream);
    }

    private Flux<PeerMessage> waitForResponses(DataInputStream dataInputStream) {
        return Flux.create((FluxSink<PeerMessage> sink) -> {
            while (!this.peerSocket.isClosed() && !this.peerSocket.isConnected()) {
                byte[] data = new byte[1024];
                try {
                    dataInputStream.read(data);
                    sink.next(PeerMessageFactory.create(this.me, this.peer, data));
                } catch (IOException e) {
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
        }).publishOn(Schedulers.single());
    }

    public Flux<PeerMessage> send(PeerMessage peerMessage) {
        try {
            this.dataOutputStream.write(peerMessage.createPacketFromObject());
            return receive();
        } catch (IOException e) {
            closeConnection();
            return Flux.error(e);
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

    public void closeConnection() {
        try {
            this.dataOutputStream.close();
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
