package main.tracker.response;

import java.util.Arrays;

public class ScrapeResponseForTorrentInfoHash {
    /**
     * I do not get torrentInfoHash in the response so I need to understand it from the order of the same request.
     */
    private final byte[] torrentInfoHash;
    private final int complete; // 	The current number of connected seeds.
    private final int downloaded; // The number of times this torrent has been downloaded.
    private final int incomplete; // The current number of connected leechers.

    public ScrapeResponseForTorrentInfoHash(byte[] torrentInfoHash, int complete, int downloaded, int incomplete) {
        this.torrentInfoHash = torrentInfoHash;
        this.complete = complete;
        this.downloaded = downloaded;
        this.incomplete = incomplete;
    }

    public byte[] getTorrentInfoHash() {
        return torrentInfoHash;
    }

    public int getComplete() {
        return complete;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public int getIncomplete() {
        return incomplete;
    }

    @Override
    public String toString() {
        return "ScrapeResponseForTorrentInfoHash{" +
                "torrentInfoHash=" + Arrays.toString(torrentInfoHash) +
                ", complete=" + complete +
                ", downloaded=" + downloaded +
                ", incomplete=" + incomplete +
                '}';
    }
}