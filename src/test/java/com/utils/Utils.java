package com.utils;

import christophedetroyer.torrent.TorrentParser;
import main.TorrentInfo;
import main.peer.PeerMessage;

import java.io.IOException;

public class Utils {
    public static TorrentInfo readTorrentFile(String torrentFilename) throws IOException {
        String torrentFilesPath = "src/test/resources/" + torrentFilename;

        return new TorrentInfo(TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static PeerMessage createFakeMessage(Class<? extends PeerMessage> messageClass) {
        return null;
    }
}
