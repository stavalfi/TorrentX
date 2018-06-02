package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.file.system.ActiveTorrents;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorAction;
import main.file.system.allocator.AllocatorReducer;
import main.file.system.allocator.AllocatorState;
import main.listener.Listener;
import main.listener.ListenerAction;
import main.listener.reducers.ListenerReducer;
import main.listener.side.effects.ListenerSideEffects;
import main.listener.state.tree.ListenerState;
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
import redux.store.StoreNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TorrentDownloaders {

	private static StoreNew<AllocatorState, AllocatorAction> allocatorStore = new StoreNew<>(new AllocatorReducer(),
			AllocatorReducer.defaultAllocatorState);

	private static StoreNew<ListenerState, ListenerAction> listenStore = new StoreNew<>(new ListenerReducer(),
			ListenerReducer.defaultListenState);

	private static Listener listener = new Listener();

	private static ListenerSideEffects listenerSideEffects = new ListenerSideEffects(listenStore);

	private List<TorrentDownloader> torrentDownloaderList = new ArrayList<>();

	public static Listener getListener() {
		return listener;
	}

	public static StoreNew<ListenerState, ListenerAction> getListenStore() {
		return listenStore;
	}

	public static ListenerSideEffects getListenerSideEffects() {
		return listenerSideEffects;
	}

	public synchronized Flux<TorrentDownloader> getTorrentDownloadersFlux() {
		// TODO: I can't go over this list and delete it in the same time. I will get concurrenctodificationException.
		// I should not save this list in the first place. for now I will copy it.

		return Flux.fromIterable(new ArrayList<>(this.torrentDownloaderList));
	}

	public synchronized TorrentDownloader createTorrentDownloader(TorrentInfo torrentInfo,
																  SearchPeers searchPeers,
																  FileSystemLink fileSystemLink,
																  BittorrentAlgorithm bittorrentAlgorithm,
																  Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore,
																  SpeedStatistics torrentSpeedStatistics,
																  TorrentStatesSideEffects torrentStatesSideEffects,
																  Flux<Link> peersCommunicatorFlux) {
		return findTorrentDownloader(torrentInfo.getTorrentInfoHash())
				.orElseGet(() -> {
					TorrentDownloader torrentDownloader = new TorrentDownloader(torrentInfo,
							searchPeers,
							fileSystemLink,
							bittorrentAlgorithm,
							torrentStatusStore,
							torrentSpeedStatistics,
							torrentStatesSideEffects, peersCommunicatorFlux);

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
		return this.torrentDownloaderList
				.stream()
				.filter(torrentDownloader -> torrentDownloader.getTorrentInfo()
						.getTorrentInfoHash().toLowerCase().equals(torrentInfoHash.toLowerCase()))
				.findFirst();
	}

	public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
		// TODO: save the initial status in the mongodb.
		Store<TorrentStatusState, TorrentStatusAction> store = new Store<>(new TorrentStatusReducer(),
				TorrentStatusReducer.defaultTorrentState,
				TorrentStatusAction::getCorrespondingIsProgressAction);
		TorrentStatesSideEffects torrentStatesSideEffects = new TorrentStatesSideEffects(torrentInfo, store);
		// TODO: remove the block()
		SearchPeers searchPeers = new SearchPeers(torrentInfo, store);

		Flux<Link> peersCommunicatorFlux =
				Flux.merge(getInstance().getListener().getPeers$(torrentInfo), searchPeers.getPeers$())
						// multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
						// multiple calls to getPeersCommunicatorFromTrackerFlux which waitForMessage new hot-flux
						// every time and then I will connect to all the peers again and again...
						.publish()
						.autoConnect(0);

		FileSystemLink fileSystemLink = ActiveTorrents.getInstance()
				.createActiveTorrentMono(torrentInfo, downloadPath, store,
						peersCommunicatorFlux.map(Link::receivePeerMessages)
								.flatMap(ReceiveMessagesNotifications::getPieceMessageResponseFlux))
				.block();

		BittorrentAlgorithm bittorrentAlgorithm =
				BittorrentAlgorithmInitializer.v1(torrentInfo,
						store,
						fileSystemLink,
						peersCommunicatorFlux);

		SpeedStatistics torrentSpeedStatistics =
				new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
						peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

		return TorrentDownloaders.getInstance()
				.createTorrentDownloader(torrentInfo,
						searchPeers,
						fileSystemLink,
						bittorrentAlgorithm,
						store,
						torrentSpeedStatistics,
						torrentStatesSideEffects,
						peersCommunicatorFlux);
	}

	private TorrentDownloaders() {
	}

	private static TorrentDownloaders instance = new TorrentDownloaders();

	public static TorrentDownloaders getInstance() {
		return instance;
	}
}
