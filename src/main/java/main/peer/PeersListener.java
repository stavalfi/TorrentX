package main.peer;

import main.App;
import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class PeersListener {
    private static Logger logger = LoggerFactory.getLogger(PeersListener.class);

    private Integer tcpPort;
    private Flux<Link> listenToIncomingPeers$;

    private FluxSink<Boolean> notifyWhenStartListenSink;
    private Flux<Boolean> notifyWhenStartListen$ =
            Flux.<Boolean>create(sink -> this.notifyWhenStartListenSink = sink)
                    .replay(1)
                    .autoConnect(0);
    private FluxSink<Boolean> doesUserWantToResumeSink;
    private Flux<Boolean> doesUserWantToResume$ =
            Flux.<Boolean>create(sink -> this.doesUserWantToResumeSink = sink)
                    .replay(1)
                    .autoConnect(0);
    private FluxSink<Boolean> notifyIfCompletelyStoppedSink;
    private Flux<Boolean> notifyIfCompletelyStoppedListen$ =
            Flux.<Boolean>create(sink -> this.notifyIfCompletelyStoppedSink = sink)
                    .replay(1)
                    .autoConnect(0);

    private FluxSink<Boolean> didWeActuallyStartedListeningSink;
    private Flux<Boolean> didWeActuallyStartedListening$ =
            Flux.<Boolean>create(sink -> this.didWeActuallyStartedListeningSink = sink)
                    .replay(1)
                    .autoConnect(0);

    public PeersListener() {
        this(AppConfig.getInstance().findFreePort());
    }

    private PeersListener(Integer tcpPort) {
        this.tcpPort = tcpPort;
        this.notifyWhenStartListenSink.next(false);
        this.doesUserWantToResumeSink.next(false);
        this.notifyIfCompletelyStoppedSink.next(false);
        this.didWeActuallyStartedListeningSink.next(false);

        this.listenToIncomingPeers$ = this.notifyWhenStartListen$
                .filter(isStarting -> isStarting)
                .take(1)
                .flatMap(__ -> Mono.<ServerSocket>create(sink -> {
                    try {
                        this.didWeActuallyStartedListeningSink.next(true);
                        sink.success(new ServerSocket(this.tcpPort));
                    } catch (IOException e) {
                        logger.error("could not start listen for new peers.", e);
                        sink.error(e);
                    }
                }))
                .flatMap(this::acceptPeersLinks)
                .subscribeOn(App.MyScheduler)
                .publish();
    }

    public synchronized void start() {
        this.notifyWhenStartListenSink.next(true);
    }

    public synchronized void resume() {
        this.doesUserWantToResumeSink.next(true);
    }

    public synchronized void pause() {
        this.doesUserWantToResumeSink.next(false);
        this.didWeActuallyStartedListeningSink.next(false);
    }

    public synchronized void stop() {
        this.notifyIfCompletelyStoppedSink.next(true);
    }

    public Mono<Boolean> isListening() {
        return this.didWeActuallyStartedListening$
                .zipWith(this.doesUserWantToResume$, (didWeStart, doesUserWantToResume) ->
                        didWeStart ? doesUserWantToResume : false)
                .take(1)
                .single();
    }

    private Flux<Link> acceptPeersLinks(ServerSocket serverSocket) {

        Flux<Socket> peersSocket = Flux.generate(sink -> {
            try {
                Socket peerSocket = serverSocket.accept();
                sink.next(peerSocket);
            } catch (IOException e) {
                closeServerSocket(serverSocket);
                sink.error(e);
            }
        });

        return peersSocket.flatMap(peerSocket -> acceptPeerConnection(peerSocket)
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty()))
                .flatMap(link ->
                        this.doesUserWantToResume$.take(1)
                                .doOnNext(isListeningToIncomingPeers -> {
                                    if (!isListeningToIncomingPeers)
                                        link.closeConnection();
                                })
                                .filter(isListeningToIncomingPeers -> isListeningToIncomingPeers)
                                .map(isListeningToIncomingPeers -> link));
    }


    private void blockThreadUntilWeStartListening() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void closeServerSocket(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException e1) {
            // TODO: do something with this shit
            //e1.printStackTrace();
        }
        this.doesUserWantToResumeSink.next(false);
        this.notifyIfCompletelyStoppedSink.next(false);
    }

    private Mono<Link> acceptPeerConnection(Socket peerSocket) {
        DataOutputStream peerDataOutputStream;
        DataInputStream peerDataInputStream;
        HandShake handShakeReceived;
        try {
            peerDataOutputStream = new DataOutputStream(peerSocket.getOutputStream());
            peerDataInputStream = new DataInputStream(peerSocket.getInputStream());

            // firstly, we need to receive Handshake message from the peer and send him Handshake back.
            handShakeReceived = new HandShake(peerDataInputStream);
        } catch (IOException e) {
            logger.error("could accept peer connection", e);
            return Mono.error(e);
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
            BadResponseException badResponseException = new BadResponseException("peer returned handshake with incorrect torrent-hash-info.");
            return Mono.error(badResponseException);
        }

        HandShake handShakeSending = new HandShake(handShakeReceived.getTorrentInfoHash(), AppConfig.getInstance().getPeerId().getBytes());
        try {
            peerDataOutputStream.write(handShakeSending.createPacketFromObject());
        } catch (IOException e) {
            return Mono.error(e);
        }
        // all went well, I accept this connection.
        Peer peer = new Peer(peerSocket.getInetAddress().getHostAddress(), peerSocket.getPort());
        return Mono.just(new Link(torrentInfo.get(), peer, peerSocket, peerDataInputStream, peerDataOutputStream));
    }

    private Optional<TorrentInfo> haveThisTorrent(String receivedTorrentInfoHash) {
        return TorrentDownloaders.getInstance()
                .findTorrentDownloader(receivedTorrentInfoHash)
                // Optional pipeline:
                .map(TorrentDownloader::getTorrentInfo);
    }

    public int getTcpPort() {
        return this.tcpPort;
    }

    public Flux<Link> getPeersConnectedToMeFlux(TorrentInfo torrentInfo) {
        return this.listenToIncomingPeers$.filter(link -> link.getTorrentInfo().equals(torrentInfo));
    }
}
