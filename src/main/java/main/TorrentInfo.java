package main;

import christophedetroyer.torrent.Torrent;
import main.tracker.Tracker;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TorrentInfo {
    private final Torrent torrent;

    public TorrentInfo(Torrent torrent) {
        this.torrent = torrent;
    }

    public String getName() {
        return this.torrent.getName();
    }


    public Long getPieceLength() {
        return this.torrent.getPieceLength();
    }

    public byte[] getPiecesBlob() {
        return this.torrent.getPiecesBlob();
    }

    public List<String> getPieces() {
        return this.torrent.getPieces();
    }

    public Long getTotalSize() {
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
        // tracker pattern example: udp://tracker.coppersurfer.tk:6969/scrape
        String trackerPattern = "^udp://(\\d*\\.)?(.*):(\\d*)(.*)?$";

        return this.torrent.getAnnounceList()
                .stream()
                .filter((String tracker) -> !tracker.equals("udp://9.rarbg.com:2710/scrape")) // problematic tracker !!!!
                .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                .filter(Matcher::matches)
                .map((Matcher matcher) -> new Tracker(matcher.group(2), Integer.parseInt(matcher.group(3))))
                .collect(Collectors.toList());
    }

    public String getTorrentInfoHash() {
        return this.torrent.getInfo_hash();
    }

}