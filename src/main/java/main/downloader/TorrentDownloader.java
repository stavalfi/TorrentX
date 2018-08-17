package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.Link;
import main.peer.SearchPeers;
import main.peer.peerMessages.PeerMessage;
import main.statistics.SpeedStatistics;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import redux.store.Store;

import java.util.AbstractMap;

public class TorrentDownloader {

    private String identifier;
    private TorrentInfo torrentInfo;
    private AllocatorStore allocatorStore;
    private SearchPeers searchPeers;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore;
    private TorrentStatesSideEffects torrentStatesSideEffects;
    private SpeedStatistics torrentSpeedStatistics;
    private Flux<Link> peersCommunicatorFlux;
    private FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages;
    private IncomingPeerMessagesNotifier incomingPeerMessagesNotifier;

    public TorrentDownloader(String identifier,
                             TorrentInfo torrentInfo,
                             AllocatorStore allocatorStore, SearchPeers searchPeers,
                             FileSystemLink fileSystemLink,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
                             SpeedStatistics torrentSpeedStatistics,
                             TorrentStatesSideEffects torrentStatesSideEffects,
                             Flux<Link> peersCommunicatorFlux,
                             FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages,
                             IncomingPeerMessagesNotifier incomingPeerMessagesNotifier) {
        this.identifier=identifier;
        this.torrentInfo = torrentInfo;
        this.allocatorStore = allocatorStore;
        this.searchPeers = searchPeers;
        this.fileSystemLink = fileSystemLink;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.torrentStatusStore = torrentStatusStore;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.torrentStatesSideEffects = torrentStatesSideEffects;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        this.emitIncomingPeerMessages = emitIncomingPeerMessages;
        this.incomingPeerMessagesNotifier = incomingPeerMessagesNotifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public SearchPeers getSearchPeers() {
        return searchPeers;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public Store<TorrentStatusState, TorrentStatusAction> getTorrentStatusStore() {
        return torrentStatusStore;
    }

    public FileSystemLink getFileSystemLink() {
        return fileSystemLink;
    }

    public SpeedStatistics getTorrentSpeedStatistics() {
        return torrentSpeedStatistics;
    }

    public Flux<Link> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public TorrentStatesSideEffects getTorrentStatesSideEffects() {
        return torrentStatesSideEffects;
    }

    public FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> getEmitIncomingPeerMessages() {
        return emitIncomingPeerMessages;
    }

    public IncomingPeerMessagesNotifier getIncomingPeerMessagesNotifier() {
        return incomingPeerMessagesNotifier;
    }

    public AllocatorStore getAllocatorStore() {
        return allocatorStore;
    }
}