package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentParser;
import main.tracker.AnnounceToTracker;
import main.tracker.ConnectToTracker;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Arrays;

class TorrentFilePrinter {
    public static void printTorrentFileInfo(String path) throws IOException {
        Torrent t1 = TorrentParser.parseTorrent(path);

        System.out.println("Created By: " + t1.getCreatedBy());
        System.out.println("Main tracker: " + t1.getAnnounce());
        if (t1.getAnnounceList() != null) {
            System.out.println("Tracker List: ");
            t1.getAnnounceList().forEach(System.out::println);
        }
        System.out.println("Comment: " + t1.getComment());
        System.out.println("Creation Date: " + t1.getCreationDate());
        System.out.println("Info_hash: " + t1.getInfo_hash());
        System.out.println("Name: " + t1.getName());
        System.out.println("Piece Length: " + t1.getPieceLength());
        System.out.println("Pieces: " + t1.getPieces());
        System.out.println("Pieces Blob: " + Arrays.toString(t1.getPiecesBlob()));
        System.out.println("Total Size: " + t1.getTotalSize());
        System.out.println("Is Single File Torrent: " + t1.isSingleFileTorrent());
        System.out.println("File List: ");
        t1.getFileList().forEach(System.out::println);
    }

    public static void printAllPeers(String path) throws Exception {
        TorrentInfo torrentInfo = new TorrentInfo(TorrentParser.parseTorrent(path));

        Flux.fromStream(torrentInfo.getTrackerList().stream())
                .flatMap(tracker -> ConnectToTracker.connect(tracker.getTracker(), tracker.getPort()))
                .flatMap((ConnectResponse response) -> AnnounceToTracker.announce(response, torrentInfo.getTorrentInfoHash()))
                .flatMap(AnnounceResponse::getPeers)
                .subscribe(System.out::println, System.out::println, System.out::println);
    }
}
