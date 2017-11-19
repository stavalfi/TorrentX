package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectionRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectionResponse;
import main.tracker.response.ScrapeResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

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
        ConnectionRequest connectionRequest = new ConnectionRequest();

        System.out.println(connectionRequest);

        ConnectionResponse connectionResponse = TrackerCommunicator.communicate(ip,port,connectionRequest);
        System.out.println(connectionResponse);

        System.out.println("------announcing-------");
        AnnounceRequest announceRequest = new AnnounceRequest(connectionResponse.getConnectionId(),TorrentHashInfo,
                "0",0,0,0,2,0,0,10,(short)8091);

        System.out.println(announceRequest);
        AnnounceResponse announceResponse = TrackerCommunicator.communicate(ip,port,announceRequest);
        System.out.println(announceResponse);
        System.out.println("peer list:");
        announceResponse.getPeers().forEach(System.out::println);

        System.out.println("------scraping-------");
        ScrapeRequest scrapeRequest = new ScrapeRequest(connectionResponse.getConnectionId(), Collections.singletonList(TorrentHashInfo));
        System.out.println(scrapeRequest);

        ScrapeResponse scrapeResponse = TrackerCommunicator.communicate(ip,port,scrapeRequest);
        System.out.println(scrapeResponse);
        System.out.println("update: ");
        scrapeResponse.getScrapeResponseForTorrentInfoHashs().forEach(System.out::println);
    }

    private static void printTorrentFileInfo() throws IOException {
        Torrent t1 = TorrentParser.parseTorrent("src/main/resources/torrent-file-example.torrent");
        System.out.println("Created By: " + t1.getCreatedBy());
        System.out.println("Main tracker: " + t1.getAnnounce());
        System.out.println("Tracker List: ");
        t1.getAnnounceList().forEach(System.out::println);
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
}
