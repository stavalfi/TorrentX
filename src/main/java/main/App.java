package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import main.requests.AnnounceRequest;
import main.requests.ConnectionRequest;
import main.requests.ScrapeRequest;
import main.response.AnnounceResponse;
import main.response.ConnectionResponse;
import main.response.ScrapeResponse;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        // udp://tracker.coppersurfer.tk:6969/announce
        // udp://9.rarbg.com:2710/announce
        // udp://p4p.arenabg.com:1337
        // udp://tracker.leechers-paradise.org:6969
        // udp://tracker.internetwarriors.net:1337
        // udp://tracker.opentrackr.org:1337/announce
        String url = "tracker.coppersurfer.tk";
        short port = 6969;
        Torrent t1 = TorrentParser.parseTorrent("src/main/resources/torrent-file-example.torrent");
        printTrackerInfo(url, port,t1.getInfo_hash());
        //printTorrentFileInfo();
    }

    private static void printTrackerInfo(String ip, short port,String TorrentHashInfo) throws IOException {

        System.out.println("------connecting-------");
        ConnectionResponse connectionResponse = TrackerCommunicator.communicate(ip,port,new ConnectionRequest());
        System.out.println(connectionResponse);
        System.out.println("------announcing-------");
        AnnounceRequest announceRequest = new AnnounceRequest(connectionResponse.getConnectionId(),TorrentHashInfo,
                "0",0,0,0,2,0,0,100,(short)8091);

        AnnounceResponse announceResponse = TrackerCommunicator.communicate(ip,port,announceRequest);
        System.out.println(announceResponse);
        System.out.println("peer list:");
        announceResponse.getPeers().forEach(System.out::println);

        System.out.println("------scraping-------");
        ScrapeRequest scrapeRequest = new ScrapeRequest(connectionResponse.getConnectionId(),TorrentHashInfo);

        ScrapeResponse scrapeResponse = TrackerCommunicator.communicate(ip,port,scrapeRequest);
    }

    private static void printTorrentFileInfo() throws IOException {
        Torrent t1 = TorrentParser.parseTorrent("src/main/resources/torrent-file-example.torrent");
        System.out.println("Created By: " + t1.getCreatedBy());
        System.out.println("Main tracker: " + t1.getAnnounce());
        System.out.println("Tracker List: ");
        t1.getAnnounceList().forEach((String tracker) -> System.out.println(tracker));
        System.out.println("Comment: " + t1.getComment());
        System.out.println("Creation Date: " + t1.getCreationDate());
        System.out.println("Info_hash: " + t1.getInfo_hash());
        System.out.println("Name: " + t1.getName());
        System.out.println("Piece Length: " + t1.getPieceLength());
        System.out.println("Pieces: " + t1.getPieces());
        System.out.println("Pieces Blob: " + t1.getPiecesBlob());
        System.out.println("Total Size: " + t1.getTotalSize());
        System.out.println("Is Single File Torrent: " + t1.isSingleFileTorrent());
        System.out.println("File List: ");
        t1.getFileList().forEach((TorrentFile torrentFile) -> System.out.println(torrentFile));
    }
}
