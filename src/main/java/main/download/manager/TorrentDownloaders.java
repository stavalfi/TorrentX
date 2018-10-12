package main.download.manager;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.allocator.AllocatorReducer;
import main.allocator.AllocatorStore;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.algorithms.IncomingPeerMessagesNotifier;
import main.peer.peerMessages.PeerMessage;
import main.peers.listener.Listener;
import main.peers.listener.ListenerAction;
import main.peers.listener.reducers.ListenerReducer;
import main.peers.listener.side.effects.ListenerSideEffects;
import main.peers.listener.state.tree.ListenerState;
import redux.store.Store;
import main.search.peers.SearchPeers;
import main.statistics.SpeedStatistics;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TorrentDownloaders {

    private static TorrentDownloaders instance = new TorrentDownloaders();

    private static AllocatorStore allocatorStore = new AllocatorStore(new Store<>(new AllocatorReducer(),
            AllocatorReducer.defaultAllocatorState, "App-Allocator-Store"));

    private static Store<ListenerState, ListenerAction> listenStore = new Store<>(new ListenerReducer(),
            ListenerReducer.defaultListenState, "App-Listener-Store");

    private static Listener listener = new Listener(allocatorStore, "App");

    private static ListenerSideEffects listenerSideEffects = new ListenerSideEffects(listenStore);

    private List<TorrentDownloader> torrentDownloaderList = new ArrayList<>();

    private TorrentDownloaders() {
    }

    public synchronized Flux<TorrentDownloader> getTorrentDownloadersFlux() {
        // TODO: I can't go over this list and delete it in the same time. I will get concurrenctodificationException.
        // I should not save this list in the first place. for now I will copy it.

        return Flux.fromIterable(new ArrayList<>(this.torrentDownloaderList));
    }

    public synchronized TorrentDownloader saveTorrentDownloader(String identifier,
                                                                TorrentInfo torrentInfo,
                                                                SearchPeers searchPeers,
                                                                FileSystemLink fileSystemLink,
                                                                BittorrentAlgorithm bittorrentAlgorithm,
                                                                Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
                                                                SpeedStatistics torrentSpeedStatistics,
                                                                TorrentStatesSideEffects torrentStatesSideEffects,
                                                                Flux<Link> peersCommunicatorFlux,
                                                                EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$,
                                                                FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages,
                                                                IncomingPeerMessagesNotifier incomingPeerMessagesNotifier) {
        return findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseGet(() -> {
                    TorrentDownloader torrentDownloader = new TorrentDownloader(identifier,
                            torrentInfo,
                            allocatorStore, searchPeers,
                            fileSystemLink,
                            bittorrentAlgorithm,
                            torrentStatusStore,
                            torrentSpeedStatistics,
                            torrentStatesSideEffects, peersCommunicatorFlux,
                            emitIncomingPeerMessages,
                            incomingPeerMessagesNotifier);

                    this.torrentDownloaderList.add(torrentDownloader);
                    return torrentDownloader;
                });
    }

    public synchronized TorrentDownloader saveTorrentDownloader(TorrentDownloader torrentDownloader) {
        return findTorrentDownloader(torrentDownloader.getTorrentInfo().getTorrentInfoHash())
                .orElseGet(() -> {
                    this.torrentDownloaderList.add(torrentDownloader);
                    return torrentDownloader;
                });
    }

    /**
     * This method is only for tests because if the client want to delete the torrent but not the file,
     * he can do that using TorrentStatusController::removeTorrent.
     * There is no reason to remove the TorrentDownloader object also.
     *
     * @param torrentInfoHash of torrent we need to delete it's TorrentDownload object
     * @return boolean which indicated if the deletion was successful.
     */
    public synchronized boolean deleteTorrentDownloader(String torrentInfoHash) {
        Optional<TorrentDownloader> torrentDownloaderOptional = findTorrentDownloader(torrentInfoHash);
        torrentDownloaderOptional.ifPresent(torrentDownloader ->
                this.torrentDownloaderList.remove(torrentDownloader));
        return torrentDownloaderOptional.isPresent();
    }

    public synchronized Optional<TorrentDownloader> findTorrentDownloader(String torrentInfoHash) {
        return this.torrentDownloaderList.stream()
                .filter(torrentDownloader -> torrentDownloader.getTorrentInfo()
                        .getTorrentInfoHash().toLowerCase().equals(torrentInfoHash.toLowerCase()))
                .findFirst();
    }

    public static TorrentDownloaders getInstance() {
        return instance;
    }

    public static AllocatorStore getAllocatorStore() {
        return allocatorStore;
    }

    public static Listener getListener() {
        return listener;
    }

    public static Store<ListenerState, ListenerAction> getListenStore() {
        return listenStore;
    }

    public static ListenerSideEffects getListenerSideEffects() {
        return listenerSideEffects;
    }
}