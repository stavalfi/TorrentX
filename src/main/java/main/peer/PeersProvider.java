package main.peer;

import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PeersProvider {
    private static Logger logger = LoggerFactory.getLogger(PeersProvider.class);

    private TorrentInfo torrentInfo;
    private TrackerProvider trackerProvider;

    public PeersProvider(TorrentInfo torrentInfo, TrackerProvider trackerProvider) {
        this.torrentInfo = torrentInfo;
        this.trackerProvider = trackerProvider;
    }

    public Mono<PeersCommunicator> connectToPeerMono(Peer peer) {
        return Mono.create((MonoSink<PeersCommunicator> sink) -> {
            Socket peerSocket = new Socket();
            sink.onCancel(() -> {
                try {
                    peerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            try {
                peerSocket.connect(new InetSocketAddress(peer.getPeerIp(), peer.getPeerPort()), 1000 * 10);
                DataInputStream receiveMessages = new DataInputStream(peerSocket.getInputStream());
                DataOutputStream sendMessages = new DataOutputStream(peerSocket.getOutputStream());

                // firstly, we need to send Handshake message to the peer and receive Handshake back.
                HandShake handShakeSending = new HandShake(HexByteConverter.hexToByte(this.torrentInfo.getTorrentInfoHash()), AppConfig.getInstance().getPeerId().getBytes());
                sendMessages.write(handShakeSending.createPacketFromObject());
                HandShake handShakeReceived = new HandShake(receiveMessages);
                String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());
                if (!this.torrentInfo.getTorrentInfoHash().toLowerCase().equals(receivedTorrentInfoHash.toLowerCase())) {
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
                    PeersCommunicator peersCommunicator = new PeersCommunicator(this.torrentInfo, peer,
                            peerSocket, receiveMessages, sendMessages);
                    sink.success(peersCommunicator);
                    return;
                }
            } catch (IOException e) {
                try {
                    peerSocket.close();
                } catch (IOException e1) {
                    // TODO: do something with this shit
                    e1.printStackTrace();
                }
                sink.error(e);
            }
        }).subscribeOn(Schedulers.elastic())
                .doOnError(PeerExceptions.communicationErrors, error -> {
                    System.out.println("failed to connect to peer: " + peer.toString() + ", reason: " + error.getClass().getName() + ", error message: " + error.getMessage());
                    logger.debug("error signal: (the application failed to connect to a peer." +
                            " the application will try to connect to the next available peer).\n" +
                            "peer: " + peer.toString() + "\n" +
                            "error message: " + error.getMessage() + ".\n" +
                            "error type: " + error.getClass().getName());
                })
                .onErrorResume(PeerExceptions.communicationErrors, error -> Mono.empty());
    }

    public Flux<Peer> getPeersFromTrackerFlux(TrackerConnection trackerConnection) {
        return trackerConnection.announceMono(torrentInfo.getTorrentInfoHash(), PeersListener.getInstance().getTcpPort())
                .flux()
                .flatMap(AnnounceResponse::getPeersFlux);
    }

    public Flux<PeersCommunicator> getPeersFromTrackerFlux(Flux<TrackerConnection> trackerConnectionFlux) {
        return trackerConnectionFlux
                .flatMap(trackerConnection -> getPeersFromTrackerFlux(trackerConnection))
                .distinct()
                .flatMap((Peer peer) -> connectToPeerMono(peer))
                .doOnNext(peersCommunicator -> System.out.println("connected to peer: " + peersCommunicator.toString()));
    }

    public Flux<PeersCommunicator> getPeersFromTrackerFlux() {
        return getPeersFromTrackerFlux(this.trackerProvider.connectToTrackersFlux());
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }
}
