package com.utils;

import christophedetroyer.torrent.TorrentParser;
import main.TorrentInfo;
import main.peer.PeersCommunicator;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.BitSet;

public class Utils {
    public static TorrentInfo readTorrentFile(String torrentFilename) throws IOException {
        String torrentFilesPath = "src/test/resources/" + torrentFilename;

        return new TorrentInfo(TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static Mono<Void> sendFakeMessage(PeerMessageType peerMessageType, PeersCommunicator peersCommunicator) {
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
}
