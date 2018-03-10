package com.utils;

import christophedetroyer.torrent.TorrentParser;
import main.TorrentInfo;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.PeerMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.BitSet;

public class Utils {
    public static TorrentInfo readTorrentFile(String torrentFilePath) throws IOException {
        String torrentFilesPath = "src/test/resources/" + torrentFilePath;

        return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static Mono<PeersCommunicator> sendFakeMessage(PeersCommunicator peersCommunicator, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return peersCommunicator.sendHaveMessage(0);
            case PortMessage:
                return peersCommunicator.sendPortMessage((short) peersCommunicator.getMe().getPeerPort());
            case ChokeMessage:
                return peersCommunicator.sendChokeMessage();
            case PieceMessage:
                return peersCommunicator.sendPieceMessage(0, 0, new byte[10]);
            case CancelMessage:
                return peersCommunicator.sendCancelMessage(0, 0, 10);
            case KeepAliveMessage:
                return peersCommunicator.sendKeepAliveMessage();
            case RequestMessage:
                return peersCommunicator.sendRequestMessage(0, 0, 10);
            case UnchokeMessage:
                return peersCommunicator.sendUnchokeMessage();
            case BitFieldMessage:
                return peersCommunicator.sendBitFieldMessage(BitSet.valueOf(new byte[10]));
            case InterestedMessage:
                return peersCommunicator.sendInterestedMessage();
            case NotInterestedMessage:
                return peersCommunicator.sendNotInterestedMessage();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static Flux<? extends PeerMessage> getSpecificMessageResponseFluxByMessageType(PeersCommunicator peersCommunicator, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return peersCommunicator.getHaveMessageResponseFlux();
            case PortMessage:
                return peersCommunicator.getPortMessageResponseFlux();
            case ChokeMessage:
                return peersCommunicator.getChokeMessageResponseFlux();
            case PieceMessage:
                return peersCommunicator.getPieceMessageResponseFlux();
            case CancelMessage:
                return peersCommunicator.getCancelMessageResponseFlux();
            case KeepAliveMessage:
                return peersCommunicator.getKeepMessageResponseFlux();
            case RequestMessage:
                return peersCommunicator.getRequestMessageResponseFlux();
            case UnchokeMessage:
                return peersCommunicator.getUnchokeMessageResponseFlux();
            case BitFieldMessage:
                return peersCommunicator.getBitFieldMessageResponseFlux();
            case InterestedMessage:
                return peersCommunicator.getInterestedMessageResponseFlux();
            case NotInterestedMessage:
                return peersCommunicator.getNotInterestedMessageResponseFlux();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }
}
