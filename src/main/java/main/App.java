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
        String TorrentFilePath = "src/main/resources/torrent-file-example.torrent";
        TorrentFilePrinter.printTorrentFileInfo(TorrentFilePath);
    }
}
