package main.file.system;

import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.torrent.status.TorrentStatusController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveTorrents {

	private List<FileSystemLinkImpl> fileSystemLinkImplTorrentList = new ArrayList<>();

	public synchronized Mono<FileSystemLinkImpl> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath,
                                                                         TorrentStatusController torrentStatusController,
                                                                         Flux<PieceMessage> peerResponsesFlux) {
		// TODO: check if this torrent exist in db.
		// firstly, check if there is an active-torrent exist already.
		// if yes, return it, else create one using the above Mono: "createActiveTorrentMono"
		return findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
				.flatMap(activeTorrentOptional -> {
					if (activeTorrentOptional.isPresent())
						return Mono.just(activeTorrentOptional.get());
					else {
						try {
							FileSystemLinkImpl fileSystemLinkImplTorrent = new FileSystemLinkImpl(torrentInfo, downloadPath,
									torrentStatusController, peerResponsesFlux);
							this.fileSystemLinkImplTorrentList.add(fileSystemLinkImplTorrent);
							return Mono.just(fileSystemLinkImplTorrent);
						} catch (IOException e) {
							return Mono.error(e);
						}
					}
				});
	}

	public synchronized boolean deleteActiveTorrentOnly(String torrentInfoHash) {
		boolean present = this.fileSystemLinkImplTorrentList.stream()
				.anyMatch(activeTorrent1 -> activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash));

		this.fileSystemLinkImplTorrentList = this.fileSystemLinkImplTorrentList.stream()
				.filter(activeTorrent1 -> !activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash))
				.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

		return present;
	}

	public synchronized Mono<Optional<FileSystemLinkImpl>> findActiveTorrentByHashMono(String torrentInfoHash) {
		Optional<FileSystemLinkImpl> first = this.fileSystemLinkImplTorrentList.stream()
				.filter(activeTorrent -> activeTorrent.getTorrentInfoHash().equals(torrentInfoHash))
				.findFirst();
		return Mono.just(first);
	}

	public synchronized Flux<FileSystemLinkImpl> getActiveTorrentsFlux() {
		// TODO: get and load all torrents from db.
		return Flux.fromIterable(this.fileSystemLinkImplTorrentList);
	}

	private static ActiveTorrents instance = new ActiveTorrents();

	public synchronized static ActiveTorrents getInstance() {
		return instance;
	}

}

