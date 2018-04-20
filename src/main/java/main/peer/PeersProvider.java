package main.peer;

import main.App;
import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import main.tracker.TrackerConnection;
import main.tracker.response.AnnounceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PeersProvider {
    private static Logger logger = LoggerFactory.getLogger(PeersProvider.class);

    private TorrentInfo torrentInfo;

    public PeersProvider(TorrentInfo torrentInfo) {
        this.torrentInfo = torrentInfo;
    }

    public Mono<Link> connectToPeerMono(Peer peer) {
        return Mono.create((MonoSink<Link> sink) -> {
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
                } else {
                    // all went well, I accept this connection.
                    Link link = new Link(this.torrentInfo, peer,
                            peerSocket, receiveMessages, sendMessages);
                    sink.success(link);
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
        }).subscribeOn(App.MyScheduler)
                .doOnError(PeerExceptions.communicationErrors, throwable -> logger.debug("error signal: (the application failed to connect to a peer." +
                        " the application will try to connect to the next available peer).\n" +
                        "peer: " + peer.toString() + "\n" +
                        "error message: " + throwable.getMessage() + ".\n" +
                        "error type: " + throwable.getClass().getName()))
                .onErrorResume(PeerExceptions.communicationErrors, error -> Mono.empty());
    }

    public Flux<Peer> getPeersFromTrackerFlux(TrackerConnection trackerConnection) {
        return trackerConnection.announceMono(torrentInfo.getTorrentInfoHash(), AppConfig.getInstance().getMyListeningPort())
                .flatMapMany(AnnounceResponse::getPeersFlux);
    }

    public ConnectableFlux<Link> getPeersCommunicatorFromTrackerFlux(Flux<TrackerConnection> trackerConnectionFlux) {
        return trackerConnectionFlux
                .flatMap(trackerConnection -> getPeersFromTrackerFlux(trackerConnection))
                .distinct()
                .flatMap((Peer peer) -> connectToPeerMono(peer))
                .publish();
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}
