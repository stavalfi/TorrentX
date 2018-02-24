package main.peer;

import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.file.ActiveTorrent;
import main.file.MyTorrents;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class InitializePeersCommunication {

    private ServerSocket listenToPeerConnection;
    private final Flux<PeersCommunicator> allPeersCommunicatorFlux;

    private InitializePeersCommunication() {

        try {
            this.listenToPeerConnection = new ServerSocket(AppConfig.getInstance().getTcpPortListeningForPeersMessages());
        } catch (IOException e) {
            // TODO: do something with this shit
            e.printStackTrace();
        }
        this.allPeersCommunicatorFlux = Flux.create((FluxSink<PeersCommunicator> sink) -> {
            while (!this.listenToPeerConnection.isClosed() && !sink.isCancelled())
                try {
                    Socket peerSocket = this.listenToPeerConnection.accept();
                    // the following method will do sink.next if the connect operation succeed.
                    acceptPeerConnection(peerSocket, sink);
                } catch (IOException e) {
                    sink.error(e);
                    try {
                        this.listenToPeerConnection.close();
                    } catch (IOException e1) {
                        // TODO: do something with this shit
                        e1.printStackTrace();
                    }
                }
        });// TODO: publicOn(Single);
    }

    public Mono<PeersCommunicator> connectToPeer(String torrentInfoHash, Peer peer) {
        return Mono.create((MonoSink<PeersCommunicator> sink) -> {
            Socket peerSocket = new Socket();
            try {
                peerSocket.connect(new InetSocketAddress(peer.getPeerIp(), peer.getPeerPort()), 1000 * 10);
                DataInputStream receiveMessages = new DataInputStream(peerSocket.getInputStream());
                OutputStream sendMessages = peerSocket.getOutputStream();

                // firstly, we need to send Handshake message to the peer and receive Handshake back.
                HandShake handShakeSending = new HandShake(HexByteConverter.hexToByte(torrentInfoHash), AppConfig.getInstance().getPeerId().getBytes());
                sendMessages.write(handShakeSending.createPacketFromObject());
                HandShake handShakeReceived = new HandShake(receiveMessages);
                String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());
                if (!torrentInfoHash.toLowerCase().equals(receivedTorrentInfoHash.toLowerCase())) {
                    // the peer sent me invalid HandShake message.
                    // by the p2p spec, I need to close to the socket.
                    sendMessages.close();
                    peerSocket.close();
                    sink.error(new BadResponseException("we sent the peer a handshake request" +
                            " and he sent us back handshake response" +
                            " with the wrong torrent-info-hash: " + receivedTorrentInfoHash));
                    return;
                } else {
                    // all went well, I accept this connection.
//                    Peer me = new Peer("localhost", peerSocket.getLocalPort());
//                    sendMessages.write(new ExtendedMessage(me, peer).createPacketFromObject());
                    sink.success(new PeersCommunicator(peer, peerSocket, receiveMessages));
                    return;
                }
            } catch (IOException e) {
                sink.error(e);
                try {
                    peerSocket.close();
                } catch (IOException e1) {
                    // TODO: do something with this shit
                    e1.printStackTrace();
                }
            }
        });
    }

    private void acceptPeerConnection(Socket peerSocket, FluxSink<PeersCommunicator> sink) {
        OutputStream dataOutputStream = null;
        try {
            dataOutputStream = peerSocket.getOutputStream();
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        DataInputStream dataInputStream = null;
        try {
            dataInputStream = new DataInputStream(peerSocket.getInputStream());
        } catch (IOException e) {
            sink.error(e);
            return;
        }

        // firstly, we need to receive Handshake message from the peer and send him Handshake back.
        HandShake handShakeReceived = null;
        try {
            handShakeReceived = new HandShake(dataInputStream);
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());
        boolean dontHaveThisTorrent = MyTorrents.getInstance()
                .getFiles()
                .map(ActiveTorrent::getTorrentInfo)
                .map(TorrentInfo::getTorrentInfoHash)
                .map(String::toLowerCase)
                .noneMatch(torrentHashInfo ->
                        torrentHashInfo.equals(receivedTorrentInfoHash.toLowerCase()));

        if (dontHaveThisTorrent) {
            // the peer sent me invalid HandShake message.
            // by the p2p spec, I need to close to the socket.
            try {
                dataInputStream.close();
                dataOutputStream.close();
                peerSocket.close();
            } catch (IOException exception) {
                //TODO: do something with this shit.
            }
            sink.error(new BadResponseException("peer returned handshake with incorrect torrent-hash-info."));
            return;
        }

        HandShake handShakeSending = new HandShake(handShakeReceived.getTorrentInfoHash(), AppConfig.getInstance().getPeerId().getBytes());
        try {
            dataOutputStream.write(handShakeSending.createPacketFromObject());
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        // all went well, I accept this connection.
        Peer peer = new Peer(peerSocket.getInetAddress().getHostAddress(), peerSocket.getPort());
        sink.next(new PeersCommunicator(peer, peerSocket, dataInputStream));
    }

    /**
     * @return hot flux!
     */
    public Flux<PeersCommunicator> listen() {
        return this.allPeersCommunicatorFlux;
    }

    public void stopListenForNewPeers() throws IOException {
        this.listenToPeerConnection.close();
    }

    private static InitializePeersCommunication instance = new InitializePeersCommunication();

    public static InitializePeersCommunication getInstance() {
        return instance;
    }
}