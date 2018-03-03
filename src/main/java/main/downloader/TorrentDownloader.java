package main.downloader;

import main.TorrentInfo;
import main.peer.ConnectToPeer;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.ReceivePeerMessages;
import main.peer.peerMessages.*;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public abstract class TorrentDownloader implements DownloadControl, ReceivePeerMessages {

    private TorrentInfo torrentInfo;
    private Downloader downloader;
    private TrackerProvider trackerProvider;
    private PeersProvider peersProvider;
    private Flux<Double> torrentDownloadSpeedFlux;
    private Flux<Double> torrentUploadSpeedFlux;
    private Flux<PeersCommunicator> peersCommunicatorFlux;
    private ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux;

    // receive messages:

    private Flux<PeerMessage> peerMessageResponseFlux;

    private Flux<BitFieldMessage> bitFieldMessageResponseFlux;
    private Flux<CancelMessage> cancelMessageResponseFlux;
    private Flux<ChokeMessage> chokeMessageResponseFlux;
    private Flux<ExtendedMessage> extendedMessageResponseFlux;
    private Flux<HaveMessage> haveMessageResponseFlux;
    private Flux<InterestedMessage> interestedMessageResponseFlux;
    private Flux<KeepAliveMessage> keepMessageResponseFlux;
    private Flux<NotInterestedMessage> notInterestedMessageResponseFlux;
    private Flux<PieceMessage> pieceMessageResponseFlux;
    private Flux<PortMessage> portMessageResponseFlux;
    private Flux<RequestMessage> requestMessageResponseFlux;
    private Flux<UnchokeMessage> unchokeMessageResponseFlux;

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader,
                             TrackerProvider trackerProvider, PeersProvider peersProvider,
                             ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux,
                             Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.downloader = downloader;
        this.trackerProvider = trackerProvider;
        this.peersProvider = peersProvider;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
        this.trackerConnectionConnectableFlux = trackerConnectionConnectableFlux;

        this.peerMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPeerMessageResponseFlux);

        this.bitFieldMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getBitFieldMessageResponseFlux);

        this.cancelMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getCancelMessageResponseFlux);

        this.chokeMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getChokeMessageResponseFlux);

        this.extendedMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getExtendedMessageResponseFlux);

        this.haveMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getHaveMessageResponseFlux);

        this.interestedMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getInterestedMessageResponseFlux);

        this.keepMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getKeepMessageResponseFlux);

        this.notInterestedMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getNotInterestedMessageResponseFlux);

        this.pieceMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPieceMessageResponseFlux);

        this.portMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPortMessageResponseFlux);

        this.requestMessageResponseFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getRequestMessageResponseFlux);

        this.unchokeMessageResponseFlux = peersCommunicatorFlux
                .flatMap(PeersCommunicator::getUnchokeMessageResponseFlux);

        this.torrentDownloadSpeedFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPeerDownloadSpeedFlux);

        this.torrentUploadSpeedFlux = this.peersCommunicatorFlux
                .flatMap(PeersCommunicator::getPeerUploadSpeedFlux);
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader,
                             ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux,
                             PeersProvider peersProvider, TrackerProvider trackerProvider) {
        this(torrentInfo, downloader, trackerProvider, peersProvider, trackerConnectionConnectableFlux,
                peersProvider.connectToPeers(trackerConnectionConnectableFlux));
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader, TrackerProvider trackerProvider) {
        this(torrentInfo,
                downloader,
                trackerProvider.connectToTrackers(),
                new PeersProvider(torrentInfo, trackerProvider, new ConnectToPeer(torrentInfo, -80)),
                trackerProvider);
    }

    public TorrentDownloader(TorrentInfo torrentInfo, Downloader downloader) {
        this(torrentInfo, downloader, new TrackerProvider(torrentInfo));
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    protected Flux<PeersCommunicator> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public Flux<Double> getTorrentDownloadSpeedFlux() {
        return torrentDownloadSpeedFlux;
    }

    public Flux<Double> getTorrentUploadSpeedFlux() {
        return torrentUploadSpeedFlux;
    }

    public ConnectableFlux<TrackerConnection> getTrackerConnectionConnectableFlux() {
        return trackerConnectionConnectableFlux;
    }

    protected PeersProvider getPeersProvider() {
        return peersProvider;
    }

    protected TrackerProvider getTrackerProvider() {
        return trackerProvider;
    }

    @Override
    public Flux<PeerMessage> getPeerMessageResponseFlux() {
        return peerMessageResponseFlux;
    }

    @Override
    public Flux<BitFieldMessage> getBitFieldMessageResponseFlux() {
        return bitFieldMessageResponseFlux;
    }

    @Override
    public Flux<CancelMessage> getCancelMessageResponseFlux() {
        return cancelMessageResponseFlux;
    }

    @Override
    public Flux<ChokeMessage> getChokeMessageResponseFlux() {
        return chokeMessageResponseFlux;
    }

    @Override
    public Flux<ExtendedMessage> getExtendedMessageResponseFlux() {
        return extendedMessageResponseFlux;
    }

    @Override
    public Flux<HaveMessage> getHaveMessageResponseFlux() {
        return haveMessageResponseFlux;
    }

    @Override
    public Flux<InterestedMessage> getInterestedMessageResponseFlux() {
        return interestedMessageResponseFlux;
    }

    @Override
    public Flux<KeepAliveMessage> getKeepMessageResponseFlux() {
        return keepMessageResponseFlux;
    }

    @Override
    public Flux<NotInterestedMessage> getNotInterestedMessageResponseFlux() {
        return notInterestedMessageResponseFlux;
    }

    @Override
    public Flux<PieceMessage> getPieceMessageResponseFlux() {
        return pieceMessageResponseFlux;
    }

    @Override
    public Flux<PortMessage> getPortMessageResponseFlux() {
        return portMessageResponseFlux;
    }

    @Override
    public Flux<RequestMessage> getRequestMessageResponseFlux() {
        return requestMessageResponseFlux;
    }

    @Override
    public Flux<UnchokeMessage> getUnchokeMessageResponseFlux() {
        return unchokeMessageResponseFlux;
    }
}