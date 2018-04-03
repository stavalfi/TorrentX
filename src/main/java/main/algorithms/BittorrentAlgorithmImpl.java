package main.algorithms;

import main.TorrentInfo;
import main.torrent.status.TorrentStatus;
import main.downloader.TorrentPieceChanged;
import main.peer.PeersCommunicator;
import main.peer.ReceiveMessages;
import main.peer.ReceiveMessagesImpl;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private TorrentInfo torrentInfo;
    private TorrentStatus torrentStatus;
    private Flux<PeersCommunicator> peersCommunicatorFlux;
    private ConnectableFlux<TorrentPieceChanged> startDownloadFlux;

    private ReceiveMessages receiveTorrentMessagesMessagesFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   TorrentStatus torrentStatus,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.torrentStatus = torrentStatus;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        this.receiveTorrentMessagesMessagesFlux = new ReceiveMessagesImpl(this.torrentInfo,
                peersCommunicatorFlux.map(PeersCommunicator::receivePeerMessages));

        this.startDownloadFlux = Flux.<TorrentPieceChanged>empty().publish();
        this.torrentStatus.getStatusTypeFlux()
                .subscribe(torrentStatusType -> {
                    switch (torrentStatusType) {
                        case STARTED:
                            this.startDownloadFlux.connect();
                    }
                });
    }


    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    @Override
    public ConnectableFlux<TorrentPieceChanged> startDownloadFlux() {
        return this.startDownloadFlux;
    }

    @Override
    public ReceiveMessages receiveTorrentMessagesMessagesFlux() {
        return this.receiveTorrentMessagesMessagesFlux;
    }
}
