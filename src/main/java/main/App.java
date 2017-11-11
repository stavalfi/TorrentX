package main;


import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import java.io.*;
import java.net.*;
import java.io.IOException;

public class App
{
    public static void main( String[] args ) throws IOException {
        printTorrentFileInfo();
    }

    private static void printTorrentFileInfo() throws IOException
    {
        Torrent t1= TorrentParser.parseTorrent("src/main/resources/torrent-file-example.torrent");
        System.out.println("Created By: " +t1.getCreatedBy());
        System.out.println("Main tracker: " +t1.getAnnounce());
        System.out.println("Tracker List: ");
        t1.getAnnounceList().forEach((String tracker)-> System.out.println(tracker));
        System.out.println("Comment: " +t1.getComment());
        System.out.println("Creation Date: " +t1.getCreationDate());
        System.out.println("Info_hash: " +t1.getInfo_hash());
        System.out.println("Name: " +t1.getName());
        System.out.println("Piece Length: " +t1.getPieceLength());
        System.out.println("Pieces: " +t1.getPieces());
        System.out.println("Pieces Blob: " +t1.getPiecesBlob());
        System.out.println("Total Size: " +t1.getTotalSize());
        System.out.println("Is Single File Torrent: " +t1.isSingleFileTorrent());
        System.out.println("File List: ");
        t1.getFileList().forEach((TorrentFile torrentFile)-> System.out.println(torrentFile));
    }
}
