package main.peer;

import main.App;
import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeersListener {
    private Integer tcpPort;
    private ServerSocket listenToPeerConnection;
    private ConnectableFlux<PeersCommunicator> peersConnectedToMeFlux;
    private AtomicBoolean didIStop = new AtomicBoolean(false);

    public PeersListener() {
        this(AppConfig.getInstance().getMyListeningPort());
    }

    public PeersListener(Integer tcpPort) {
        this.tcpPort = tcpPort;
        this.peersConnectedToMeFlux = Flux.create((FluxSink<PeersCommunicator> sink) -> {
            try {
                this.listenToPeerConnection = new ServerSocket(this.tcpPort);
            } catch (IOException e) {
                // TODO: do something with this shit
                e.printStackTrace();
                sink.error(e);
                return;
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
                        //e1.printStackTrace();
                    }
                    if (!this.didIStop.get())
                        sink.error(e);
                    return;
                }
        }).subscribeOn(App.MyScheduler)
                .publish();
    }

    private void acceptPeerConnection(Socket peerSocket, FluxSink<PeersCommunicator> sink) {
        DataOutputStream peerDataOutputStream;
        DataInputStream peerDataInputStream;
        HandShake handShakeReceived;
        try {
            peerDataOutputStream = new DataOutputStream(peerSocket.getOutputStream());
            peerDataInputStream = new DataInputStream(peerSocket.getInputStream());

            // firstly, we need to receive Handshake message from the peer and send him Handshake back.
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
        return TorrentDownloaders.getInstance()
                .findTorrentDownloader(receivedTorrentInfoHash)
                // Optional pipeline:
                .map(TorrentDownloader::getTorrentInfo);
    }

    public void stopListenForNewPeers() throws IOException {
        if (this.didIStop.compareAndSet(false, true) &&
                this.listenToPeerConnection != null) {
            this.listenToPeerConnection.close();
        }
    }

    public int getTcpPort() {
        return this.tcpPort;
    }

    public ConnectableFlux<PeersCommunicator> getPeersConnectedToMeFlux() {
        return peersConnectedToMeFlux;
    }
}
