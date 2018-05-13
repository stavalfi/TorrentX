package main;

import christophedetroyer.torrent.Torrent;
import christophedetroyer.torrent.TorrentFile;
import main.tracker.Tracker;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Claim: piece length is integer and not long.
// Prof:
// (1) piece size can't be more than Integer.MAX_VALUE because the protocol allow me to request
// even the last byte of a piece and if the piece size is more than Integer.MAX_VALUE,
// than I can't specify the "begin" of that request using 4 bytes.
// (2) in the tests, a block size is bounded to the allocatedBlock size and it's integer so
// we can't create a request for a block larger than Integer.MAX_VALUE. so for both reasons,
// a request of a block can't be more than Integer.MAX_VALUE.

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

    public boolean isSingleFileTorrent() {
        return this.torrent.isSingleFileTorrent();
    }

    public int getPieceLength(int pieceIndex) {
        long totalSize = getTotalSize();
        int pieceLength = this.torrent.getPieceLength().intValue();
        int piecesAmount = this.torrent.getPieces().size() - 1;
        if (pieceIndex == this.torrent.getPieces().size() - 1) {
            long lastPieceLength = totalSize - (piecesAmount * pieceLength);
            return (int) lastPieceLength;
        }
        return pieceLength;
    }

    public byte[] getPiecesBlob() {
        return this.torrent.getPiecesBlob();
    }

    public List<String> getPieces() {
        return this.torrent.getPieces();
    }

    public long getTotalSize() {
        return this.getFileList()
                .stream()
                .mapToLong(TorrentFile::getFileLength)
                .sum();
    }

    public long getPieceStartPosition(int pieceIndex) {
        long totalSize = getTotalSize();
        long thisPieceLength = getPieceLength(pieceIndex);
        long position = pieceIndex < getPieces().size() - 1 ?
                pieceIndex * this.torrent.getPieceLength() :
                totalSize - thisPieceLength;
        return position;
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

        return this.torrent.getAnnounceList() == null ?
                Collections.emptyList() :
                this.torrent.getAnnounceList().stream()
                        .filter((String tracker) -> !tracker.equals("udp://9.rarbg.com:2710/scrapeMono")) // problematic tracker !!!!
                        .map((String tracker) -> Pattern.compile(trackerPattern).matcher(tracker))
                        .filter(Matcher::matches)
                        .map((Matcher matcher) -> new Tracker(matcher.group(1), matcher.group(3), Integer.parseInt(matcher.group(4))))
                        .filter(tracker -> !tracker.getConnectionType().equals("http"))
                        .filter(tracker -> !tracker.getConnectionType().equals("https"))
                        .collect(Collectors.toList());
    }

    public String getTorrentInfoHash() {
        return this.torrent.getInfo_hash();
    }

    public List<TorrentFile> getFileList() {
        if (isSingleFileTorrent()) {
            Long totalSize = this.torrent.getTotalSize();
            return Collections.singletonList(new TorrentFile(totalSize, Collections.singletonList(getName())));
        }
        return this.torrent.getFileList();
    }

    @Override
    public String toString() {

        String trackers = getTrackerList()
                .stream()
                .map(tracker -> tracker.toString())
                .collect(Collectors.joining("\n"));

        String fileList = getFileList()
                .stream()
                .map(TorrentFile::toString)
                .collect(Collectors.joining("\n"));

        return "TorrentInfo{" +
                "Created By: " + this.torrent.getCreatedBy() + "\n" +
                "Main tracker: " + this.torrent.getAnnounce() + "\n" +
                "Comment: " + this.torrent.getComment() + "\n" +
                "Info_hash: " + this.torrent.getInfo_hash() + "\n" +
                "Name: " + this.torrent.getName() + "\n" +
                "Piece Length: " + this.torrent.getPieceLength() + "\n" +
                "Last Piece Length: " + this.getPieceLength(this.torrent.getPieces().size() - 1) + "\n" +
                "Pieces: " + this.torrent.getPieces().size() + "\n" +
                "Total Size: " + this.getTotalSize() + "\n" +
                "Is Single File Torrent: " + this.isSingleFileTorrent() + "\n" +
                "File List:\n" + fileList + "\n" +
                "Tracker List: \n" + trackers;
    }

    public String getTorrentFilePath() {
        return torrentFilePath;
    }
}