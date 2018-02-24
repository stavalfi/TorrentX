package main.peer;

import main.AppConfig;
import main.HexByteConverter;
import main.MyTorrents;
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
            while (!this.listenToPeerConnection.isClosed())
                try {
                    Socket peerSocket = this.listenToPeerConnection.accept();
                    sink.next(acceptPeerConnection(peerSocket));
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

    private PeersCommunicator acceptPeerConnection(Socket peerSocket) throws IOException {
        OutputStream dataOutputStream = peerSocket.getOutputStream();
        DataInputStream dataInputStream = new DataInputStream(peerSocket.getInputStream());

        // firstly, we need to receive Handshake message from the peer and send him Handshake back.
        HandShake handShakeReceived = new HandShake(dataInputStream);
        String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());
        boolean dontHaveThisTorrent = MyTorrents.myTorrents
                .stream()
                .noneMatch(torrentInfo ->
                        torrentInfo.getTorrentInfoHash()
                                .toLowerCase()
                                .equals(receivedTorrentInfoHash.toLowerCase()));
        if (dontHaveThisTorrent) {
            // the peer sent me invalid HandShake message.
            // by the p2p spec, I need to close to the socket.
            dataInputStream.close();
            dataOutputStream.close();
            peerSocket.close();
        }

        HandShake handShakeSending = new HandShake(handShakeReceived.getTorrentInfoHash(), AppConfig.getInstance().getPeerId().getBytes());
        dataOutputStream.write(handShakeSending.createPacketFromObject());
        // all went well, I accept this connection.
        Peer peer = new Peer(peerSocket.getInetAddress().getHostAddress(), peerSocket.getPort());
        return new PeersCommunicator(peer, peerSocket, dataInputStream);
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