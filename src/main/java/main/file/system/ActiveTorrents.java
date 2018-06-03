package main.file.system;

import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveTorrents {

	private List<FileSystemLink> fileSystemLinkImplTorrentList = new ArrayList<>();

	public synchronized Mono<FileSystemLink> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath,
																	 Store<TorrentStatusState, TorrentStatusAction> store,
																	 Flux<PieceMessage> peerResponsesFlux) {
		// TODO: check if this torrent exist in db.
		// firstly, check if there is an active-torrent exist already.
		// if yes, return it, else waitForMessage one using the above Mono: "createActiveTorrentMono"
		return findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
				.flatMap(activeTorrentOptional -> activeTorrentOptional
						.map(Mono::just)
						.orElse(FileSystemLinkImpl.create(torrentInfo, downloadPath, store, peerResponsesFlux)
								.doOnNext(this.fileSystemLinkImplTorrentList::add)));
	}

	public synchronized boolean deleteActiveTorrentOnly(String torrentInfoHash) {
		boolean present = this.fileSystemLinkImplTorrentList.stream()
				.anyMatch(activeTorrent -> activeTorrent.getTorrentInfo().getTorrentInfoHash().equals(torrentInfoHash));

		this.fileSystemLinkImplTorrentList = this.fileSystemLinkImplTorrentList.stream()
				.filter(activeTorrent -> !activeTorrent.getTorrentInfo().getTorrentInfoHash().equals(torrentInfoHash))
				.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

		return present;
	}

	public synchronized Mono<Optional<FileSystemLink>> findActiveTorrentByHashMono(String torrentInfoHash) {
		Optional<FileSystemLink> first = this.fileSystemLinkImplTorrentList.stream()
				.filter(activeTorrent -> activeTorrent.getTorrentInfo().getTorrentInfoHash().equals(torrentInfoHash))
				.findFirst();
		return Mono.just(first);
	}

	public synchronized Flux<FileSystemLink> getActiveTorrentsFlux() {
		// TODO: get and load all torrents from db.
		return Flux.fromIterable(this.fileSystemLinkImplTorrentList);
	}

	private static ActiveTorrents instance = new ActiveTorrents();

	public synchronized static ActiveTorrents getInstance() {
		return instance;
	}

}

