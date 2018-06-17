package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.SearchPeers;
import main.statistics.SpeedStatistics;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

public class TorrentDownloader {

	private TorrentInfo torrentInfo;
	private SearchPeers searchPeers;
	private FileSystemLink fileSystemLink;
	private BittorrentAlgorithm bittorrentAlgorithm;
	private Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore;
	private TorrentStatesSideEffects torrentStatesSideEffects;
	private SpeedStatistics torrentSpeedStatistics;
	private Flux<Link> peersCommunicatorFlux;

	public TorrentDownloader(TorrentInfo torrentInfo,
							 SearchPeers searchPeers,
							 FileSystemLink fileSystemLink,
							 BittorrentAlgorithm bittorrentAlgorithm,
							 Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
							 SpeedStatistics torrentSpeedStatistics,
							 TorrentStatesSideEffects torrentStatesSideEffects,
							 Flux<Link> peersCommunicatorFlux) {
		this.torrentInfo = torrentInfo;
		this.searchPeers = searchPeers;
		this.fileSystemLink = fileSystemLink;
		this.bittorrentAlgorithm = bittorrentAlgorithm;
		this.torrentStatusStore = torrentStatusStore;
		this.torrentSpeedStatistics = torrentSpeedStatistics;
		this.torrentStatesSideEffects = torrentStatesSideEffects;
		this.peersCommunicatorFlux = peersCommunicatorFlux;
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
}