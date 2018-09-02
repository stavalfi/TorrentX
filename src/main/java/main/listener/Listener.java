package main.listener;

import main.AppConfig;
import main.HexByteConverter;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.allocator.AllocatorStore;
import main.listener.state.tree.ListenerState;
import main.peer.BadTorrentInfoHashHandShakeException;
import main.peer.Link;
import main.peer.Peer;
import main.peer.PeerExceptions;
import main.peer.peerMessages.HandShake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import redux.store.Store;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Listener {
    private static Logger logger = LoggerFactory.getLogger(Listener.class);
    private static Scheduler listenerScheduler = Schedulers.newSingle("LISTENER");

    private static final int TCP_PORT = 8040;

    private Flux<ServerSocket> startListen$;
    private Flux<Link> resumeListen$;
    private Flux<ListenerState> pauseListen$;
    private Flux<ListenerState> restartListener$;
    private AllocatorStore allocatorStore;
    private String identifier;

    public Listener(AllocatorStore allocatorStore, String identifier) {
        this.identifier = identifier;
        this.allocatorStore = allocatorStore;
        Store<ListenerState, ListenerAction> listenerStore = TorrentDownloaders.getListenStore();

        Supplier<Mono<ServerSocket>> serverSocketSupplier = () -> {
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                return Mono.just(serverSocket);
            } catch (IOException e) {
                return Mono.error(e);
            }
        };

        this.startListen$ = listenerStore.statesByAction(ListenerAction.START_LISTENING_IN_PROGRESS)
                .concatMap(__ -> serverSocketSupplier.get())
                .doOnError(throwable -> logger.error(this.identifier + " - failed to create ServerSocket object.", throwable))
//                .onErrorResume(PeerExceptions.communicationErrors,
//                        throwable -> listenerStore.dispatch(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
//                                .flatMap(__ -> Mono.empty()))
                .concatMap(serverSocket -> listenerStore.dispatch(ListenerAction.START_LISTENING_SELF_RESOLVED)
                        .filter(listenerState -> listenerState.fromAction(ListenerAction.START_LISTENING_SELF_RESOLVED) ||
                                listenerState.fromAction(ListenerAction.START_LISTENING_WIND_UP))
                        .map(__ -> serverSocket))
                .doOnNext(serverSocket -> logger.info(this.identifier + " - created server-socket under port: " + serverSocket.getLocalPort() + " (not listening to new incoming peers)."))
                .replay(1)
                .autoConnect(0);

        this.resumeListen$ = listenerStore.statesByAction(ListenerAction.RESUME_LISTENING_IN_PROGRESS)
                .concatMap(__ -> this.startListen$.take(1))
                .concatMap(serverSocket -> listenerStore.dispatch(ListenerAction.RESUME_LISTENING_SELF_RESOLVED).filter(listenerState -> listenerState.fromAction(ListenerAction.RESUME_LISTENING_SELF_RESOLVED)).map(__ -> serverSocket))
                .doOnNext(serverSocket -> logger.info(this.identifier + " - resume listening to incoming peers under port: " + serverSocket.getLocalPort()))
                .concatMap(serverSocket -> listenerStore.notifyWhen(ListenerAction.RESUME_LISTENING_WIND_UP, serverSocket))
                .concatMap(this::acceptPeersLinks)
                .concatMap(link -> listenerStore.notifyWhen(ListenerAction.RESUME_LISTENING_WIND_UP, link))
                .doOnError(throwable -> logger.error(this.identifier + " - fatal error while accepting peer connection or in server-socket object", throwable))
                .onErrorResume(PeerExceptions.communicationErrors, __ -> Mono.empty())
                .doOnError(error -> logger.error(this.identifier + " - internal error in Listener module: " + error))
                .publish()
                .autoConnect(0);

        this.pauseListen$ = listenerStore.statesByAction(ListenerAction.PAUSE_LISTENING_IN_PROGRESS)
                .concatMap(__ -> getListeningPort())
                .concatMap(listeningPort -> listenerStore.dispatch(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED)
                        .filter(listenerState -> listenerState.fromAction(ListenerAction.PAUSE_LISTENING_SELF_RESOLVED))
                        .doOnNext(serverSocket -> logger.info(this.identifier + " - paused listening to incoming peers under port: " + listeningPort)))
                .publish()
                .autoConnect(0);

        Function<ServerSocket, Mono<ServerSocket>> closeServerSocket = serverSocket -> {
            try {
                serverSocket.close();
                logger.info(this.identifier + " - closed the app listening-for-new-peers socket.");
                return Mono.just(serverSocket);
            } catch (IOException e) {
                logger.error(this.identifier + " - fatal error while closing server-socket object under port " + serverSocket.getLocalPort() + ": " + e);
                return Mono.error(e);
            }
        };

        this.restartListener$ = listenerStore.statesByAction(ListenerAction.RESTART_LISTENING_IN_PROGRESS)
                .concatMap(__ -> this.startListen$.take(1))
                .concatMap(closeServerSocket)
                .concatMap(__ -> listenerStore.dispatch(ListenerAction.RESTART_LISTENING_SELF_RESOLVED))
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
                // isClosed()==true means that only I caused the serverSocket to be closed (TODO: is it true???).
                if (e instanceof SocketException && serverSocket.isClosed()) {
                    logger.info(this.identifier + " - Listener socket is closed due to explicit closing by the app or the user. - " + e);
                    sink.complete();
                } else {
                    logger.error(this.identifier + " - Listener socket is closed due to internal error: ", e);
                    sink.error(e);
                }
            }
        });

        return peersSocket.subscribeOn(listenerScheduler)
                .concatMap(peerSocket -> acceptPeerConnection(peerSocket)
                        .doOnNext(link -> logger.info(this.identifier + " - new peer connected to me successfully: " + link))
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
            logger.error(this.identifier + " - could accept peer connection", e);
            return Mono.error(e);
        }

        String receivedTorrentInfoHash = HexByteConverter.byteToHex(handShakeReceived.getTorrentInfoHash());

        Optional<TorrentDownloader> torrentDownloader = haveThisTorrent(receivedTorrentInfoHash);

        if (!torrentDownloader.isPresent()) {
            // the peer sent me invalid HandShake message.
            // by the p2p spec, I need to close to the socket.
            try {
                peerDataInputStream.close();
                peerDataOutputStream.close();
                peerSocket.close();
            } catch (IOException exception) {
                //TODO: do something with this shit.
            }
            BadTorrentInfoHashHandShakeException badResponseException =
                    new BadTorrentInfoHashHandShakeException("peer returned handshake with torrent-hash-info " +
                            "of torrent which this app doesn't have.");
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
        return Mono.just(new Link(this.allocatorStore,
                torrentDownloader.get().getTorrentInfo(),
                peer,
                peerSocket,
                peerDataInputStream,
                peerDataOutputStream,
                this.identifier,
                torrentDownloader.get().getEmitIncomingPeerMessages()));
    }

    private Optional<TorrentDownloader> haveThisTorrent(String receivedTorrentInfoHash) {
        return TorrentDownloaders.getInstance().findTorrentDownloader(receivedTorrentInfoHash);
    }

    public Mono<Integer> getListeningPort() {
        return this.startListen$.take(1)
                .map(ServerSocket::getLocalPort)
                .single();
    }

    public Flux<Link> getPeers$(TorrentInfo torrentInfo) {
        // TODO: we need to complete this flux when the torrent is removed. need to add test for it.
        return this.resumeListen$
                .filter(link -> link.getTorrentInfo().equals(torrentInfo));
    }

    public Flux<ListenerState> getPauseListen$() {
        return pauseListen$;
    }

    public Flux<ListenerState> getRestartListener$() {
        return restartListener$;
    }

    public Flux<ServerSocket> getStartListen$() {
        return startListen$;
    }

    public Flux<Link> getResumeListen$() {
        return resumeListen$;
    }
}
