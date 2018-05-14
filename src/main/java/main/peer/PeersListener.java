package main.peer;

import main.AppConfig;
import main.torrent.status.TorrentStatusStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeersListener {
    private static Logger logger = LoggerFactory.getLogger(PeersListener.class);

    private Integer tcpPort;
    private ServerSocket listenToPeerConnection;
    private ConnectableFlux<Link> listenToIncomingPeers$;
    private AtomicBoolean isStoppedListenForNewPeers = new AtomicBoolean(false);
    private TorrentStatusStore torrentStatusStore;

    public PeersListener(TorrentStatusStore torrentStatusStore) {
        this(torrentStatusStore, AppConfig.getInstance().getMyListeningPort());
    }

    private PeersListener(TorrentStatusStore torrentStatusStore, Integer tcpPort) {
        this.tcpPort = tcpPort;
        this.torrentStatusStore = torrentStatusStore;

        Mono<ServerSocket> serverSocketMono = Mono.create(sink -> {
            try {
                sink.success(new ServerSocket(this.tcpPort));
            } catch (IOException e) {
                logger.error("could not start listen for new peers.", e);
                sink.error(e);
            }
        });

        // TODO: uncomment
//        this.listenToIncomingPeers$ = serverSocketMono.flatMap(serverSocket ->
//                this.torrentStatusStore.changeState(Action.START_LISTENING_TO_INCOMING_PEERS)
//                        .map(status -> serverSocket))
//                .doOnNext(serverSocket -> this.listenToPeerConnection = serverSocket)
//                .flatMapMany(this::acceptPeersLinks)
//                .subscribeOn(App.MyScheduler)
//                .publish();
    }
    // TODO: uncomment

//    private Flux<Link> acceptPeersLinks(ServerSocket serverSocket) {
//        // update the status that we started listening to incoming peers.
//        // TODO: uncomment
////        CompletableFuture.runAsync(() -> blockThreadUntilWeStartListening())
////                .thenRun(() -> this.torrentStatusStore.changeState(Action.RESUME_LISTENING_TO_INCOMING_PEERS).block());
//
//        Flux<Socket> peersSocket = Flux.generate(sink -> {
//            try {
//                Socket peerSocket = serverSocket.accept();
//                sink.next(peerSocket);
//            } catch (IOException e) {
//                closeServerSocket(serverSocket);
//                sink.error(e);
//            }
//        });
//
//        return peersSocket.flatMap(peerSocket -> acceptPeerConnection(peerSocket))
//                .doOnError(throwable -> logger.error("could accept peer connection", throwable))
//                // TODO: uncomment
////                .doOnError(throwable -> this.torrentStatusStore.changeState(Action.PAUSE_LISTENING_TO_INCOMING_PEERS).block())
//                .flatMap(link ->
//                        this.torrentStatusStore.getLatestState$()
//                                .map(Status::isListeningToIncomingPeers)
//                                .doOnNext(isListeningToIncomingPeers -> {
//                                    if (!isListeningToIncomingPeers)
//                                        link.closeConnection();
//                                })
//                                .filter(isListeningToIncomingPeers -> isListeningToIncomingPeers)
//                                .map(isListeningToIncomingPeers -> link));
//    }
//
//
//    private void blockThreadUntilWeStartListening() {
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void closeServerSocket(ServerSocket serverSocket) {
//        try {
//            serverSocket.close();
//        } catch (IOException e1) {
//            // TODO: do something with this shit
//            //e1.printStackTrace();
//        }
//    }
//
//    private Mono<Link> acceptPeerConnection(Socket peerSocket) {
//        DataOutputStream peerDataOutputStream;
//        DataInputStream peerDataInputStream;
//        HandShake handShakeReceived;
//        try {
//            peerDataOutputStream = new DataOutputStream(peerSocket.getOutputStream());
//            peerDataInputStream = new DataInputStream(peerSocket.getInputStream());
//
//            // firstly, we need to receive Handshake message from the peer and send him Handshake back.
//            handShakeReceived = new HandShake(peerDataInputStream);
//        } catch (IOException e) {
//            return Mono.error(e);
//        }
//
//        String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());
//
//        Optional<TorrentInfo> torrentInfo = haveThisTorrent(receivedTorrentInfoHash);
//
//        if (!torrentInfo.isPresent()) {
//            // the peer sent me invalid HandShake message.
//            // by the p2p spec, I need to close to the socket.
//            try {
//                peerDataInputStream.close();
//                peerDataOutputStream.close();
//                peerSocket.close();
//            } catch (IOException exception) {
//                //TODO: do something with this shit.
//            }
//            BadResponseException badResponseException = new BadResponseException("peer returned handshake with incorrect torrent-hash-info.");
//            return Mono.error(badResponseException);
//        }
//
//        HandShake handShakeSending = new HandShake(handShakeReceived.getTorrentInfoHash(), AppConfig.getInstance().getPeerId().getBytes());
//        try {
//            peerDataOutputStream.write(handShakeSending.createPacketFromObject());
//        } catch (IOException e) {
//            return Mono.error(e);
//        }
//        // all went well, I accept this connection.
//        Peer peer = new Peer(peerSocket.getInetAddress().getHostAddress(), peerSocket.getPort());
//        return Mono.just(new Link(torrentInfo.get(), peer, peerSocket, peerDataInputStream, peerDataOutputStream));
//    }
//
//    private Optional<TorrentInfo> haveThisTorrent(String receivedTorrentInfoHash) {
//        return TorrentDownloaders.getInstance()
//                .findTorrentDownloader(receivedTorrentInfoHash)
//                // Optional pipeline:
//                .map(TorrentDownloader::getTorrentInfo);
//    }
//
//    public Mono<Status> stopListenForNewPeers() {
//        return Mono.<PeersListener>create(sink -> {
//            if (this.isStoppedListenForNewPeers.compareAndSet(false, true)) {
//                try {
//                    this.listenToPeerConnection.close();
//                    sink.success(this);
//                    return;
//                } catch (IOException e) {
//                    sink.error(e);
//                    return;
//                }
//            }
//            sink.error(new Exception("you tried to stop listen for new peers but we already stopped."));
//        }).flatMap(peersListener -> pauseListenToIncomingPeers());
//    }
//
//    public Mono<Status> resumeListenToIncomingPeers() {
//        return Mono.fromCallable(() -> this.isStoppedListenForNewPeers.get())
//                .filter(isStoppedListenForNewPeers -> !isStoppedListenForNewPeers)
//                .flatMap(__ -> this.torrentStatusStore.changeState(Action.RESUME_LISTENING_TO_INCOMING_PEERS));
//    }
//
//    public Mono<Status> pauseListenToIncomingPeers() {
//        return Mono.fromCallable(() -> this.isStoppedListenForNewPeers.get())
//                .filter(isStoppedListenForNewPeers -> !isStoppedListenForNewPeers)
//                .flatMap(__ -> this.torrentStatusStore.changeState(Action.PAUSE_LISTENING_TO_INCOMING_PEERS));
//    }
//
//    public Mono<Status> startListenToIncomingPeers() {
//        return Mono.fromCallable(() -> this.isStoppedListenForNewPeers.get())
//                .filter(isStoppedListenForNewPeers -> !isStoppedListenForNewPeers)
//                .flatMap(__ -> this.torrentStatusStore.changeState(Action.START_LISTENING_TO_INCOMING_PEERS));
//    }
//
    public int getTcpPort() {
        return this.tcpPort;
    }

    public Flux<Link> getPeersConnectedToMeFlux() {
        return this.listenToIncomingPeers$;
    }
}
