package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import main.tracker.Tracker;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TorrentInfo {
    private String torrentFilePath;
    private final Torrent torrent;

    public TorrentInfo(String torrentFilePath, Torrent torrent) {
        this.torrentFilePath = torrentFilePath;
        this.torrent = torrent;
    }

    public TorrentInfo(TorrentInfo torrentInfo) {
        this.torrentFilePath = torrentInfo.getTorrentFilePath();
        this.torrent = torrentInfo.torrent;
    }

    public String getName() {
        return this.torrent.getName();
    }


    public int getPieceLength() {
        return Math.toIntExact(this.torrent.getPieceLength());
    }

    public byte[] getPiecesBlob() {
        return this.torrent.getPiecesBlob();
    }

    public List<String> getPieces() {
        return this.torrent.getPieces();
    }

    public long getTotalSize() {
        return this.torrent.getTotalSize();
    }


    public String getComment() {
        return this.torrent.getComment();
    }


    public String getCreatedBy() {
        return this.torrent.getCreatedBy();
    }


    public Date getCreationDate() {
        return this.torrent.getCreationDate();
    }


    public List<Tracker> getTrackerList() {
        // tracker pattern example: udp://tracker.coppersurfer.tk:6969/scrapeMono
        String trackerPattern = "^(.*)://(\\d*\\.)?(.*):(\\d*)(.*)?$";

        return this.torrent.getAnnounceList()
                .stream()
                .filter((String tracker) -> !tracker.equals("udp://9.rarbg.com:2710/scrapeMono")) // problematic tracker !!!!
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .map((Matcher matcher) -> new Tracker(matcher.group(1), matcher.group(3), Integer.parseInt(matcher.group(4))))
                .collect(Collectors.toList());
    }

    public String getTorrentInfoHash() {
        return this.torrent.getInfo_hash();
    }

    public List<TorrentFile> getFileList() {
        return this.torrent.getFileList();
    }

    @Override
    public String toString() {

        String trackers = this.torrent.getAnnounceList() != null ?
                this.torrent.getAnnounceList()
                        .stream()
                        .collect(Collectors.joining("\n")) : "";

        String fileList = this.torrent.getFileList() != null ?
                this.torrent.getFileList()
                        .stream()
                        .map(TorrentFile::toString)
                        .collect(Collectors.joining("\n")) : "";

        return "TorrentInfo{" +
                "Created By: " + this.torrent.getCreatedBy() + "\n" +
                "Main tracker: " + this.torrent.getAnnounce() + "\n" +
                "Comment: " + this.torrent.getComment() + "\n" +
                "Info_hash: " + this.torrent.getInfo_hash() + "\n" +
                "Name: " + this.torrent.getName() + "\n" +
                "Piece Length: " + this.torrent.getPieceLength() + "\n" +
                "Pieces: " + this.torrent.getPieces().size() + "\n" +
                "Total Size: " + this.torrent.getTotalSize() + "\n" +
                "Is Single File Torrent: " + this.torrent.isSingleFileTorrent() + "\n" +
                "File List:\n" + fileList + "\n" +
                "Tracker List: \n" + trackers;
    }

    public String getTorrentFilePath() {
        return torrentFilePath;
    }
}