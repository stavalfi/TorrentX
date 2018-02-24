package main.downloader;

import main.TorrentInfo;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class TorrentDownloader implements DownloadControl {

    private TorrentInfo torrentInfo;
    private ConnectableFlux<PeersCommunicator> peersCommunicatorFlux;
    private Flux<PeerMessage> peerResponseFlux; // to read all peer messages.
    private Downloader downloader;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             ConnectableFlux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.downloader = new Downloader(this.torrentInfo.getTorrentInfoHash());
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.peerResponseFlux = this.peersCommunicatorFlux
                .flatMap(peersCommunicator -> peersCommunicator.receive());
    }

    public TorrentDownloader(TorrentInfo torrentInfo) {
        this(torrentInfo, PeersProvider.connectToPeers(torrentInfo));
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public Flux<PeerMessage> getPeerResponseFlux() {
        return this.peerResponseFlux;
    }

    public ConnectableFlux<PeersCommunicator> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    @Override
    public void start() {
        // listen to piece messages to save them in local machine
        Flux<PieceMessage> pieceMessageFlux = this.peerResponseFlux
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class);

        this.downloader.downloadAsync(pieceMessageFlux);

        this.peersCommunicatorFlux.connect();
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void remove() {

    }
}