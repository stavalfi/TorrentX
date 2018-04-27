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

	private List<ActiveTorrent> activeTorrentList = new ArrayList<>();

	public synchronized Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath,
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
							ActiveTorrent activeTorrent = new ActiveTorrent(torrentInfo, downloadPath,
									torrentStatusController, peerResponsesFlux);
							this.activeTorrentList.add(activeTorrent);
							return Mono.just(activeTorrent);
						} catch (IOException e) {
							return Mono.error(e);
						}
					}
				});
	}

	public synchronized boolean deleteActiveTorrentOnly(String torrentInfoHash) {
		boolean present = this.activeTorrentList.stream()
				.anyMatch(activeTorrent1 -> activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash));

		this.activeTorrentList = this.activeTorrentList.stream()
				.filter(activeTorrent1 -> !activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash))
				.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

		return present;
	}

	public synchronized Mono<Optional<ActiveTorrent>> findActiveTorrentByHashMono(String torrentInfoHash) {
		Optional<ActiveTorrent> first = this.activeTorrentList.stream()
				.filter(activeTorrent -> activeTorrent.getTorrentInfoHash().equals(torrentInfoHash))
				.findFirst();
		return Mono.just(first);
	}

	public synchronized Flux<ActiveTorrent> getActiveTorrentsFlux() {
		// TODO: get and load all torrents from db.
		return Flux.fromIterable(this.activeTorrentList);
	}

	private static ActiveTorrents instance = new ActiveTorrents();

	public synchronized static ActiveTorrents getInstance() {
		return instance;
	}

}

