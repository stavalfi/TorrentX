package main.algorithms;

import main.TorrentInfo;
import main.downloader.DownloadControl;
import main.downloader.TorrentPieceChanged;
import main.peer.PeersCommunicator;
import main.peer.ReceiveMessages;
import main.peer.ReceiveMessagesImpl;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private TorrentInfo torrentInfo;
    private DownloadControl downloadControl;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    private ReceiveMessages receiveTorrentMessagesMessagesFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   DownloadControl downloadControl,
                                   Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.downloadControl = downloadControl;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        Flux<ReceiveMessages> receiveMessagesFlux = peersCommunicatorFlux
                .map(PeersCommunicator::receivePeerMessages);
        this.receiveTorrentMessagesMessagesFlux = new ReceiveMessagesImpl(this.torrentInfo, receiveMessagesFlux);
    }


    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    @Override
    public Flux<TorrentPieceChanged> downloadAsync() {
        return Flux.never();
    }

    @Override
    public ReceiveMessages receiveTorrentMessagesMessagesFlux() {
        return this.receiveTorrentMessagesMessagesFlux;
    }
}
