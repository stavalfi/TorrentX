package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentParser;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class App {
    public static void main(String[] args) throws IOException {
        String myPeerId = "-AZ5750-TpkXttZLfpSH";
        Torrent t1 = TorrentParser.parseTorrent("src/main/resources/torrent-file-example.torrent");
        printTorrentFileInfo(t1);
//        printTrackerInfo("tracker.coppersurfer.tk", (short) 6969, t1.getInfo_hash(), myPeerId);
//        printTrackerInfo("p4p.arenabg.com", (short) 1337, t1.getInfo_hash(), myPeerId);
        printTrackerInfo("tracker.leechers-paradise.org", (short) 6969, t1.getInfo_hash(), myPeerId);
//        printTrackerInfo("tracker.internetwarriors.net", (short) 1337, t1.getInfo_hash(), myPeerId);
//        printTrackerInfo("tracker.opentrackr.org", (short) 1337, t1.getInfo_hash(), myPeerId);

        System.out.println();
        System.out.println("----------------------------------");
        System.out.println("----------------------------------");
        System.out.println("----------------------------------");
        System.out.println("----------------------------------");
        System.out.println();

        // 87.219.121.218, tcpPort=53497

        String peerIp = "87.219.121.218";
        int peerPort = 53497;

        //PeerCommunicator.sendMessage(peerIp, peerPort, new HandShake(HexByteConverter.hexToByte(t1.getInfo_hash()), myPeerId.getBytes()));
    }


    private static void printTrackerInfo(String ip, short port, String TorrentHashInfo, String myPeerId) throws IOException {

        System.out.println("----------------------------------");
        System.out.println("----------------------------------");
        System.out.println(ip);
        System.out.println("----------------------------------");
        System.out.println("----------------------------------");

        System.out.println("------connecting-------");
        ConnectRequest connectRequest = new ConnectRequest(123456);

        System.out.println(connectRequest);

        ConnectResponse connectResponse = TrackerCommunicator.communicate(ip, port, connectRequest);
        System.out.println(connectResponse);

        System.out.println("------announcing-------");
        // TODO: change 8091
        short tcpPortImListeningOn = 8091;
        int maxPeersIWantFromTracker = 1000;
        byte[] torrentHashInfoAsByteArray = HexByteConverter.hexToByte(TorrentHashInfo);
        AnnounceRequest announceRequest = new AnnounceRequest(connectResponse.getConnectionId(), 123456, torrentHashInfoAsByteArray,
                myPeerId.getBytes(), 0, 0, 0, 2, 0, 0, maxPeersIWantFromTracker, tcpPortImListeningOn);

        System.out.println(announceRequest);
        AnnounceResponse announceResponse = TrackerCommunicator.communicate(ip, port, announceRequest);
        System.out.println(announceResponse);
        System.out.println("peer list:");
        announceResponse.getPeers().forEach(System.out::println);

        System.out.println("------scraping-------");
        ScrapeRequest scrapeRequest = new ScrapeRequest(connectResponse.getConnectionId(), 123456, Collections.singletonList(torrentHashInfoAsByteArray));
        System.out.println(scrapeRequest);

        ScrapeResponse scrapeResponse = TrackerCommunicator.communicate(ip, port, scrapeRequest);
        System.out.println(scrapeResponse);
        System.out.println("update: ");
        scrapeResponse.getScrapeResponseForTorrentInfoHashs().forEach(System.out::println);
    }

    private static void printTorrentFileInfo(Torrent t1) throws IOException {
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
}
