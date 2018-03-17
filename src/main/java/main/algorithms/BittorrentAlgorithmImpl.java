package main.algorithms;

import main.TorrentInfo;
import main.downloader.TorrentPiece;
import main.peer.PeersCommunicator;
import main.peer.ReceivePeerMessages;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;

public class BittorrentAlgorithmImpl implements BittorrentAlgorithm {
    private TorrentInfo torrentInfo;
    private Flux<PeersCommunicator> peersCommunicatorFlux;

    private final Flux<PeerMessage> peerMessageResponseFlux;
    private final Flux<BitFieldMessage> bitFieldMessageResponseFlux;
    private final Flux<CancelMessage> cancelMessageResponseFlux;
    private final Flux<ChokeMessage> chokeMessageResponseFlux;
    private final Flux<ExtendedMessage> extendedMessageResponseFlux;
    private final Flux<HaveMessage> haveMessageResponseFlux;
    private final Flux<InterestedMessage> interestedMessageResponseFlux;
    private final Flux<KeepAliveMessage> keepMessageResponseFlux;
    private final Flux<NotInterestedMessage> notInterestedMessageResponseFlux;
    private final Flux<PieceMessage> pieceMessageResponseFlux;
    private final Flux<PortMessage> portMessageResponseFlux;
    private final Flux<RequestMessage> requestMessageResponseFlux;
    private final Flux<UnchokeMessage> unchokeMessageResponseFlux;

    public BittorrentAlgorithmImpl(TorrentInfo torrentInfo,
                                   final Flux<PeersCommunicator> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.peersCommunicatorFlux = peersCommunicatorFlux;

        this.peerMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getPeerMessageResponseFlux);

        this.bitFieldMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getBitFieldMessageResponseFlux);

        this.cancelMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getCancelMessageResponseFlux);

        this.chokeMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getChokeMessageResponseFlux);

        this.extendedMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getExtendedMessageResponseFlux);

        this.haveMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getHaveMessageResponseFlux);

        this.interestedMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getInterestedMessageResponseFlux);

        this.keepMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getKeepMessageResponseFlux);

        this.notInterestedMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getNotInterestedMessageResponseFlux);

        this.pieceMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getPieceMessageResponseFlux);

        this.portMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getPortMessageResponseFlux);

        this.requestMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getRequestMessageResponseFlux);

        this.unchokeMessageResponseFlux = peersCommunicatorFlux
                .flatMap(ReceivePeerMessages::getUnchokeMessageResponseFlux);
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    @Override
    public Flux<TorrentPiece> downloadAsync() {
        return Flux.never();
    }
}
