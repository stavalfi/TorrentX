package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentParser;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import org.joou.UShort;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.joou.Unsigned.ushort;

public class TorrentFilePrinter {
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
        Torrent t1 = TorrentParser.parseTorrent(path);
        byte[] torrentInfoHash = HexByteConverter.hexToByte(t1.getInfo_hash());
        byte[] peerIdAsByteArray = "-AZ5750-TpkXttZLfpSH".getBytes();
        String trackerPattern = "^udp://(\\d*\\.)?(.*):(\\d*)(.*)?$";
        t1.getAnnounceList()
                .stream()
                .filter((String tracker) -> !tracker.equals("udp://9.rarbg.com:2710/announce")) // problematic tracker !!!!
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .map((Matcher matcher) -> new AbstractMap.SimpleEntry<String, UShort>(matcher.group(2), ushort(matcher.group(3))))
//                .map(tracker -> {
//                    ConnectResponse connectResponse = TrackerCommunicator.communicate(tracker.getKey(), tracker.getValue().intValue(), new ConnectRequest(123456));
//                    AnnounceRequest announceRequest = new AnnounceRequest(connectResponse.getConnectionId(), 123456, torrentInfoHash, peerIdAsByteArray,
//                            0, 0, 0, 2, 0, 0, 1000, (short) 8091);
//                    return TrackerCommunicator.communicate(tracker.getKey(), tracker.getValue().intValue(), announceRequest);
//                })
//                .flatMap(res -> res.getPeers().stream())
                .forEach(System.out::println);
    }
}
