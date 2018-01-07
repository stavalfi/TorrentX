package main;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        String TorrentFilePath = "src/main/resources/torrent-file-example.torrent";
        TorrentFilePrinter.printTorrentFileInfo(TorrentFilePath);
    }
}
