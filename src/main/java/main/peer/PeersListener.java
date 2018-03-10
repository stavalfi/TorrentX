package main.peer;

import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class PeersListener {
    private Integer tcpPort = 80;
    private ServerSocket listenToPeerConnection;
    private ConnectableFlux<PeersCommunicator> peersConnectedToMeFlux;

    private PeersListener() {

        this.peersConnectedToMeFlux = Flux.create((FluxSink<PeersCommunicator> sink) -> {
            try {
                this.listenToPeerConnection = new ServerSocket(this.tcpPort);
            } catch (IOException e) {
                // TODO: do something with this shit
                e.printStackTrace();
            }
            while (!this.listenToPeerConnection.isClosed() && !sink.isCancelled())
                try {
                    Socket peerSocket = this.listenToPeerConnection.accept();
                    // the following method will do sink.next if the connect operation succeed.
                    acceptPeerConnection(peerSocket, sink);
                } catch (IOException e) {
                    try {
                        this.listenToPeerConnection.close();
                    } catch (IOException e1) {
                        // TODO: do something with this shit
                        e1.printStackTrace();
                    }
                    sink.error(e);
                }
        }).subscribeOn(Schedulers.elastic())
                .publish();
    }

    private void acceptPeerConnection(Socket peerSocket, FluxSink<PeersCommunicator> sink) {
        DataOutputStream peerDataOutputStream = null;
        try {
            peerDataOutputStream = new DataOutputStream(peerSocket.getOutputStream());
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        DataInputStream peerDataInputStream = null;
        try {
            peerDataInputStream = new DataInputStream(peerSocket.getInputStream());
        } catch (IOException e) {
            sink.error(e);
            return;
        }

        // firstly, we need to receive Handshake message from the peer and send him Handshake back.
        HandShake handShakeReceived = null;
        try {
            handShakeReceived = new HandShake(peerDataInputStream);
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());

        Optional<TorrentInfo> torrentInfo = haveThisTorrent(receivedTorrentInfoHash);

        if (!torrentInfo.isPresent()) {
            // the peer sent me invalid HandShake message.
            // by the p2p spec, I need to close to the socket.
            try {
                peerDataInputStream.close();
                peerDataOutputStream.close();
                peerSocket.close();
            } catch (IOException exception) {
                //TODO: do something with this shit.
            }
            sink.error(new BadResponseException("peer returned handshake with incorrect torrent-hash-info."));
            return;
        }

        HandShake handShakeSending = new HandShake(handShakeReceived.getTorrentInfoHash(), AppConfig.getInstance().getPeerId().getBytes());
        try {
            peerDataOutputStream.write(handShakeSending.createPacketFromObject());
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        // all went well, I accept this connection.
        Peer peer = new Peer(peerSocket.getInetAddress().getHostAddress(), peerSocket.getPort());
        sink.next(new PeersCommunicator(torrentInfo.get(), peer, peerSocket, peerDataInputStream, peerDataOutputStream));
    }

    private Optional<TorrentInfo> haveThisTorrent(String receivedTorrentInfoHash) {
        return Optional.empty();
    }

    public void stopListenForNewPeers() throws IOException, NullPointerException {
        this.listenToPeerConnection.close();
    }

    public int getTcpPort() {
        return this.tcpPort;
    }


    private static PeersListener instance = new PeersListener();

    public static PeersListener getInstance() {
        return instance;
    }

    public ConnectableFlux<PeersCommunicator> getPeersConnectedToMeFlux() {
        return peersConnectedToMeFlux;
    }
}
