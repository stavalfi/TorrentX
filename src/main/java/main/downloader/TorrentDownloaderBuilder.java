package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.file.system.FileSystemLink;
import main.file.system.FileSystemLinkImpl;
import main.file.system.allocator.AllocatorStore;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.IncomingPeerMessagesNotifierImpl;
import main.peer.Link;
import main.peer.SearchPeers;
import main.peer.peerMessages.PeerMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.*;
import redux.store.Store;

import java.util.AbstractMap;
import java.util.Objects;

// TODO: save the initial status in the mongodb.
// TODO: in case the test doesn't want the SearchPeers to get more peers from the tracker, I need to take care of it.
public class TorrentDownloaderBuilder {
    private TorrentInfo torrentInfo;
    private SearchPeers searchPeers;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore;
    private TorrentStatesSideEffects torrentStatesSideEffects;
    private SpeedStatistics torrentSpeedStatistics;
    private Flux<Link> peersCommunicatorFlux;
    private AllocatorStore allocatorStore;
    private String identifier;
    private EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$;
    private FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages;
    private IncomingPeerMessagesNotifier incomingPeerMessagesNotifier;


    private TorrentDownloaderBuilder(TorrentInfo torrentInfo, String identifier) {
        this.torrentInfo = torrentInfo;
        this.identifier = identifier;
    }

    public static TorrentDownloaderBuilder builder(TorrentInfo torrentInfo, String identifier) {
        assert torrentInfo != null;

        return new TorrentDownloaderBuilder(torrentInfo, identifier);
    }

    public static TorrentDownloader buildDefault(TorrentInfo torrentInfo, String identifier, String downloadPath) {
        return builder(torrentInfo, identifier)
                .setToDefaultIncomingPeerMessages()
                .setToDefaultEmitIncomingPeerMessages()
                .setToDefaultAllocatorStore()
                .setToDefaultTorrentStatusStore(identifier)
                .setToDefaultTorrentStatesSideEffects()
                .setToDefaultSearchPeers()
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultFileSystemLink(downloadPath)
                .setToDefaultBittorrentAlgorithm()
                .setToDefaultTorrentSpeedStatistics()
                .build();
    }

    public TorrentDownloader build() {
        return new TorrentDownloader(this.identifier,
                this.torrentInfo,
                this.allocatorStore,
                this.searchPeers,
                this.fileSystemLink,
                this.bittorrentAlgorithm,
                this.torrentStatusStore,
                this.torrentSpeedStatistics,
                this.torrentStatesSideEffects,
                this.peersCommunicatorFlux,
                this.emitIncomingPeerMessages,
                incomingPeerMessagesNotifier);
    }

    public TorrentDownloaderBuilder setIncomingPeerMessages(EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$) {
        this.incomingPeerMessages$ = incomingPeerMessages$;
        this.incomingPeerMessagesNotifier = new IncomingPeerMessagesNotifierImpl(this.incomingPeerMessages$);
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultIncomingPeerMessages() {
        this.incomingPeerMessages$ = EmitterProcessor.create();
        this.incomingPeerMessagesNotifier = new IncomingPeerMessagesNotifierImpl(this.incomingPeerMessages$);
        return this;
    }

    public TorrentDownloaderBuilder setEmitIncomingPeerMessages(FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this.emitIncomingPeerMessages = emitIncomingPeerMessages;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultEmitIncomingPeerMessages() {
        this.emitIncomingPeerMessages = this.incomingPeerMessages$.sink();
        return this;
    }

    public TorrentDownloaderBuilder setAllocatorStore(AllocatorStore allocatorStore) {
        this.allocatorStore = allocatorStore;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultAllocatorStore() {
        this.allocatorStore = TorrentDownloaders.getAllocatorStore();
        return this;
    }

    public TorrentDownloaderBuilder setSearchPeers(SearchPeers searchPeers) {
        this.searchPeers = searchPeers;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultSearchPeers() {
        Objects.requireNonNull(this.torrentStatusStore);
        Objects.requireNonNull(this.allocatorStore);
        Objects.requireNonNull(this.incomingPeerMessages$);
        Objects.requireNonNull(this.emitIncomingPeerMessages);

        this.searchPeers = new SearchPeers(this.allocatorStore, this.torrentInfo, this.torrentStatusStore, this.identifier, this.emitIncomingPeerMessages);
        return this;
    }

    public TorrentDownloaderBuilder setFileSystemLink(FileSystemLink fileSystemLink) {
        this.fileSystemLink = fileSystemLink;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultFileSystemLink(String downloadPath) {
        Objects.requireNonNull(this.torrentStatusStore);
        Objects.requireNonNull(this.allocatorStore);
        Objects.requireNonNull(this.incomingPeerMessagesNotifier);

        this.fileSystemLink = new FileSystemLinkImpl(this.torrentInfo, downloadPath, this.allocatorStore, this.torrentStatusStore,
                this.incomingPeerMessagesNotifier.getPieceMessageResponseFlux(), this.identifier);
        return this;
    }

    public TorrentDownloaderBuilder setBittorrentAlgorithm$(BittorrentAlgorithm bittorrentAlgorithm$) {
        this.bittorrentAlgorithm = bittorrentAlgorithm$;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultBittorrentAlgorithm() {
        Objects.requireNonNull(this.allocatorStore);
        Objects.requireNonNull(this.torrentInfo);
        Objects.requireNonNull(this.torrentStatusStore);
        Objects.requireNonNull(this.fileSystemLink);
        Objects.requireNonNull(this.incomingPeerMessagesNotifier);
        Objects.requireNonNull(this.peersCommunicatorFlux);
        Objects.requireNonNull(this.identifier);

        this.bittorrentAlgorithm = BittorrentAlgorithmInitializer.v1(this.allocatorStore,
                this.torrentInfo,
                this.torrentStatusStore,
                this.fileSystemLink,
                this.incomingPeerMessagesNotifier,
                this.peersCommunicatorFlux,
                this.identifier);
        return this;
    }

    public TorrentDownloaderBuilder setTorrentStatusStore(Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore) {
        this.torrentStatusStore = torrentStatusStore;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultTorrentStatusStore(String identifier) {
        this.torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, identifier);
        return this;
    }

    public TorrentDownloaderBuilder setTorrentStatesSideEffects(TorrentStatesSideEffects torrentStatesSideEffects) {
        this.torrentStatesSideEffects = torrentStatesSideEffects;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultTorrentStatesSideEffects() {
        Objects.requireNonNull(this.torrentStatusStore);

        this.torrentStatesSideEffects = new TorrentStatesSideEffects(this.torrentInfo, this.torrentStatusStore);
        return this;
    }

    public TorrentDownloaderBuilder setTorrentSpeedStatistics(SpeedStatistics torrentSpeedStatistics) {
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultTorrentSpeedStatistics() {
        Objects.requireNonNull(this.peersCommunicatorFlux);

        this.torrentSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo,
                this.peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));
        return this;
    }

    public TorrentDownloaderBuilder setPeersCommunicatorFlux(Flux<Link> peersCommunicatorFlux) {
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        return this;
    }

    public TorrentDownloaderBuilder setToDefaultPeersCommunicatorFlux() {
        Objects.requireNonNull(this.searchPeers);

        this.peersCommunicatorFlux = Flux.merge(TorrentDownloaders.getListener().getPeers$(this.torrentInfo), this.searchPeers.getPeers$())
                // multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
                // multiple calls to connectToPeers$ which waitForMessage new hot-flux
                // every time and then I will connect to all the peers again and again...
                .publish()
                .autoConnect(0);
        return this;
    }
}
