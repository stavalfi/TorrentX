package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPieceChanged;
import main.peer.PeersCommunicator;
import main.peer.ReceivedMessagesImpl;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private Flux<TorrentPieceChanged> startDownloadFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {

        ReceivedMessagesImpl receiveTorrentMessagesMessagesFlux = new ReceivedMessagesImpl(
                peersCommunicatorFlux.map(PeersCommunicator::receivePeerMessages));

        this.startDownloadFlux = startBittorrentAlgorithm(torrentInfo, torrentStatus, peersCommunicatorFlux)
                .publish();

        ((ConnectableFlux<TorrentPieceChanged>) this.startDownloadFlux).connect();

    }

    private Flux<TorrentPieceChanged> startBittorrentAlgorithm(TorrentInfo torrentInfo,
                                                               TorrentStatus torrentStatus,
                                                               Flux<PeersCommunicator> peersCommunicatorFlux) {
        return Flux.empty();
    }

    @Override
    public Flux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }
}
