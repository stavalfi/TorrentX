package main.listen;

import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.listen.state.tree.ListenerState;
import main.peer.Link;
import main.peer.Peer;
import main.peer.PeerExceptions;
import main.peer.peerMessages.HandShake;
import main.tracker.BadResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Listener {
    private static Logger logger = LoggerFactory.getLogger(Listener.class);

    private final int TCP_PORT = 8040;

    private Flux<ServerSocket> startListen$;
    private Flux<Link> resumeListen$;
    private Flux<ListenerState> restartListener$;

    public Listener() {
        ListenerStore listenerStore = TorrentDownloaders.getInstance()
                .getListenStore();

        Supplier<Mono<ServerSocket>> serverSocketSupplier = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(TCP_PORT);
                return Mono.just(serverSocket);
            } catch (IOException e) {
                return Mono.error(e);
            }
        };

        this.startListen$ = listenerStore.getByAction$(ListenerAction.START_LISTENING_IN_PROGRESS)
                .flatMap(__ -> serverSocketSupplier.get())
                .doOnError(throwable -> logger.error("failed to create ServerSocket object.", throwable))
                .onErrorResume(PeerExceptions.communicationErrors,
                        throwable -> listenerStore.dispatch(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                                .flatMap(__ -> Mono.empty()))
                .flatMap(serverSocket -> listenerStore.dispatch(ListenerAction.START_LISTENING_SELF_RESOLVED)
                        .filter(listenerState -> listenerState.fromAction(ListenerAction.START_LISTENING_SELF_RESOLVED) ||
                                listenerState.fromAction(ListenerAction.START_LISTENING_WIND_UP))
                        .map(__ -> serverSocket))
                .doOnNext(serverSocket -> logger.info("created server-socket under port: " + getTcpPort()))
                .replay(1)
                .autoConnect(0);


        this.resumeListen$ = listenerStore.getByAction$(ListenerAction.RESUME_LISTENING_IN_PROGRESS)
                .flatMap(__ -> startListen$.take(1))
                .flatMap(serverSocket -> listenerStore.dispatch(ListenerAction.RESUME_LISTENING_SELF_RESOLVED)
                        .filter(listenerState -> listenerState.fromAction(ListenerAction.RESUME_LISTENING_SELF_RESOLVED))
                        .map(__ -> serverSocket))
                .doOnNext(serverSocket -> logger.info("started listening to incoming peers under port: " + getTcpPort()))
                .flatMap(serverSocket -> listenerStore.notifyWhen(ListenerAction.RESUME_LISTENING_WIND_UP, serverSocket))
                .flatMap(serverSocket -> acceptPeersLinks(serverSocket))
                .flatMap(link -> listenerStore.notifyWhen(ListenerAction.RESUME_LISTENING_WIND_UP, link))
                .doOnError(throwable -> logger.error("fatal error while accepting peer connection or in server-socket object", throwable))
                .onErrorResume(PeerExceptions.communicationErrors,
                        throwable -> listenerStore.dispatch(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                                .flatMap(__ -> Mono.empty()))
                .publish()
                .autoConnect(0);

        Function<ServerSocket, Mono<Void>> closeServerSocket = serverSocket -> {
            try {
                serverSocket.close();
                return Mono.empty();
            } catch (IOException e) {
                return Mono.error(e);
            }
        };

        this.restartListener$ = listenerStore.getByAction$(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                .flatMap(__ -> startListen$)
                .flatMap(closeServerSocket)
                .flatMap(serverSocket -> listenerStore.dispatch(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
                .filter(listenerState -> listenerState.fromAction(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
                .publish()
                .autoConnect(0);
    }

    private Flux<Link> acceptPeersLinks(ServerSocket serverSocket) {

        Flux<Socket> peersSocket = Flux.generate(sink -> {
            try {
                Socket peerSocket = serverSocket.accept();
                // TODO: check which errors indicate that the peer
                // closed and which errors indicate that the ServerSocket is corrupted.
                sink.next(peerSocket);
            } catch (IOException e) {
                sink.error(e);
            }
        });

        return peersSocket.flatMap(peerSocket -> acceptPeerConnection(peerSocket)
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty()));
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
        return TCP_PORT;
    }

    public Flux<Link> getPeers$(TorrentInfo torrentInfo) {
        return resumeListen$.filter(link -> link.getTorrentInfo().equals(torrentInfo));
    }

    private static Listener instance = new Listener();

    public static Listener getInstance() {
        return instance;
    }
}
