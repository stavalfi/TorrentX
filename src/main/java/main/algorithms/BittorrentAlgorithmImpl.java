package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPiece;
import main.peer.PeersCommunicator;
import main.peer.ReceiveMessages;
import main.peer.ReceiveMessagesImpl;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private TorrentInfo torrentInfo;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    private ReceiveMessages receiveTorrentMessagesMessagesFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   final Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        Flux<ReceiveMessages> receiveMessagesFlux = peersCommunicatorFlux
                .map(PeersCommunicator::receivePeerMessages);
        this.receiveTorrentMessagesMessagesFlux = new ReceiveMessagesImpl(this.torrentInfo, receiveMessagesFlux);
    }


    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    @Override
    public Flux<TorrentPiece> downloadAsync() {
        return Flux.never();
    }

    @Override
    public ReceiveMessages receiveTorrentMessagesMessagesFlux() {
        return this.receiveTorrentMessagesMessagesFlux;
    }
}
