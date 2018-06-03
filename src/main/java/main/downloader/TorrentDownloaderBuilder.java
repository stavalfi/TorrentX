package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.file.system.ActiveTorrents;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.ReceiveMessagesNotifications;
import main.peer.SearchPeers;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import redux.store.Store;

// TODO: save the initial status in the mongodb.
public class TorrentDownloaderBuilder {
	private TorrentInfo torrentInfo;
	private SearchPeers searchPeers;
	private FileSystemLink fileSystemLink;
	private BittorrentAlgorithm bittorrentAlgorithm;
	private Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore;
	private TorrentStatesSideEffects torrentStatesSideEffects;
	private SpeedStatistics torrentSpeedStatistics;
	private Flux<Link> peersCommunicatorFlux;

	private TorrentDownloaderBuilder(TorrentInfo torrentInfo) {
		this.torrentInfo = torrentInfo;
	}

	public static TorrentDownloaderBuilder builder(TorrentInfo torrentInfo) {
		assert torrentInfo != null;
		return new TorrentDownloaderBuilder(torrentInfo);
	}

	public static TorrentDownloader buildDefault(TorrentInfo torrentInfo, String downloadPath) {
		return builder(torrentInfo)
				.setToDefaultTorrentStatusStore()
				.setToDefaultTorrentStatesSideEffects()
				.setToDefaultSearchPeers()
				.setToDefaultPeersCommunicatorFlux()
				.setToDefaultFileSystemLink(downloadPath)
				.setToDefaultBittorrentAlgorithm()
				.setToDefaultTorrentSpeedStatistics()
				.build();
	}

	public TorrentDownloader build() {
		return new TorrentDownloader(this.torrentInfo,
				this.searchPeers,
				this.fileSystemLink,
				this.bittorrentAlgorithm,
				this.torrentStatusStore,
				this.torrentSpeedStatistics,
				this.torrentStatesSideEffects,
				this.peersCommunicatorFlux);
	}

	public TorrentDownloaderBuilder setSearchPeers(SearchPeers searchPeers) {
		this.searchPeers = searchPeers;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultSearchPeers() {
		assert this.torrentStatusStore != null;
		this.searchPeers = new SearchPeers(this.torrentInfo, this.torrentStatusStore);
		return this;
	}

	public TorrentDownloaderBuilder setFileSystemLink(FileSystemLink fileSystemLink) {
		this.fileSystemLink = fileSystemLink;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultFileSystemLink(String downloadPath) {
		assert this.torrentStatusStore != null;
		this.fileSystemLink = ActiveTorrents.getInstance()
				.createActiveTorrentMono(torrentInfo, downloadPath, this.torrentStatusStore,
						this.peersCommunicatorFlux.map(Link::receivePeerMessages)
								.flatMap(ReceiveMessagesNotifications::getPieceMessageResponseFlux))
				.block();
		return this;
	}

	public TorrentDownloaderBuilder setBittorrentAlgorithm(BittorrentAlgorithm bittorrentAlgorithm) {
		this.bittorrentAlgorithm = bittorrentAlgorithm;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultBittorrentAlgorithm() {
		assert this.torrentStatusStore != null;
		this.bittorrentAlgorithm = BittorrentAlgorithmInitializer.v1(torrentInfo,
				this.torrentStatusStore,
				fileSystemLink,
				peersCommunicatorFlux);
		return this;
	}

	public TorrentDownloaderBuilder setTorrentStatusStore(Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore) {
		this.torrentStatusStore = torrentStatusStore;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultTorrentStatusStore() {
		this.torrentStatusStore = new Store<>(new TorrentStatusReducer(),
				TorrentStatusReducer.defaultTorrentState);
		return this;
	}

	public TorrentDownloaderBuilder setTorrentStatesSideEffects(TorrentStatesSideEffects torrentStatesSideEffects) {
		this.torrentStatesSideEffects = torrentStatesSideEffects;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultTorrentStatesSideEffects() {
		assert this.torrentStatusStore != null;
		this.torrentStatesSideEffects = new TorrentStatesSideEffects(this.torrentInfo, this.torrentStatusStore);
		return this;
	}

	public TorrentDownloaderBuilder setTorrentSpeedStatistics(SpeedStatistics torrentSpeedStatistics) {
		this.torrentSpeedStatistics = torrentSpeedStatistics;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultTorrentSpeedStatistics() {
		assert this.peersCommunicatorFlux != null;
		this.torrentSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo,
				this.peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));
		return this;
	}

	public TorrentDownloaderBuilder setPeersCommunicatorFlux(Flux<Link> peersCommunicatorFlux) {
		this.peersCommunicatorFlux = peersCommunicatorFlux;
		return this;
	}

	public TorrentDownloaderBuilder setToDefaultPeersCommunicatorFlux() {
		assert this.searchPeers != null;
		this.peersCommunicatorFlux = Flux.merge(TorrentDownloaders.getListener().getPeers$(this.torrentInfo), this.searchPeers.getPeers$())
				// multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
				// multiple calls to getPeersCommunicatorFromTrackerFlux which waitForMessage new hot-flux
				// every time and then I will connect to all the peers again and again...
				.publish()
				.autoConnect(0);
		return this;
	}
}
