package com.utils;

import christophedetroyer.torrent.TorrentParser;
import main.TorrentInfo;
import main.peer.Peer;
import main.peer.peerMessages.*;

import java.io.IOException;
import java.util.BitSet;

public class Utils {
    public static TorrentInfo readTorrentFile(String torrentFilename) throws IOException {
        String torrentFilesPath = "src/test/resources/" + torrentFilename;

        return new TorrentInfo(TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static PeerMessage createFakeMessage(PeerMessageType peerMessageType, Peer from, Peer to) {
        switch (peerMessageType) {
            case HaveMessage:
                return new HaveMessage(from, to, 0);
            case PortMessage:
                return new PortMessage(from, to, (short) from.getPeerPort());
            case ChokeMessage:
                return new ChokeMessage(from, to);
            case PieceMessage:
                return new PieceMessage(from, to, 0, 0, new byte[10]);
            case CancelMessage:
                return new CancelMessage(from, to, 0, 0, 10);
            case KeepAliveMessage:
                return new KeepAliveMessage(from, to);
            case RequestMessage:
                return new RequestMessage(from, to, 0, 0, 10);
            case UnchokeMessage:
                return new UnchokeMessage(from, to);
            case BitFieldMessage:
                return new BitFieldMessage(from, to, BitSet.valueOf(new byte[10]));
            case InterestedMessage:
                return new InterestedMessage(from, to);
            case NotInterestedMessage:
                return new NotInterestedMessage(from, to);
            case ExtendedMessage:
                return new ExtendedMessage(from, to, new byte[10]);
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }
}
