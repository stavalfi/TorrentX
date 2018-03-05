package main.tracker.response;

import main.tracker.Tracker;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

public class ScrapeResponse extends TrackerResponse {

    private final List<ScrapeResponseForTorrentInfoHash> ScrapeResponseForTorrentInfoHashes;

    public ScrapeResponse(Tracker tracker, List<ScrapeResponseForTorrentInfoHash> scrapeResponseForTorrentInfoHashes) {
        super(tracker);
        ScrapeResponseForTorrentInfoHashes = scrapeResponseForTorrentInfoHashes;
    }

    public List<ScrapeResponseForTorrentInfoHash> getScrapeResponseForTorrentInfoHashes() {
        return ScrapeResponseForTorrentInfoHashes;
    }

    @Override
    public String toString() {
        return "ScrapeResponse{" +
                "ScrapeResponseForTorrentInfoHashes=" + ScrapeResponseForTorrentInfoHashes +
                "} " + super.toString();
    }

    /**
     * Offset      Size            Name            Value
     * 0           32-bit integer  action          2 // scrapeMono
     * 4           32-bit integer  transaction_id
     * 8 + 12 * n  32-bit integer  downloaded
     * ?           32-bit integer  complete
     * ?           32-bit integer  incomplete
     * 8 + 12 * N
     *
     * @param response          what the tracker sent me back.
     * @param torrentInfoHashes what torrents I asked about in the scrapeMono request.
     *                          The order is important and I assume that the answer
     *                          will be with the same order as this list.
     */
    public ScrapeResponse(Tracker tracker, byte[] response, List<byte[]> torrentInfoHashes) {
        super(tracker);
        ByteBuffer receiveData = ByteBuffer.wrap(response);
        setActionNumber(receiveData.getInt());
        assert getActionNumber() == 2;
        setTransactionId(receiveData.getInt());

        this.ScrapeResponseForTorrentInfoHashes = torrentInfoHashes.stream()
                .map((byte[] torrentInfoHash) -> new ScrapeResponseForTorrentInfoHash
                        (torrentInfoHash, receiveData.getInt(), receiveData.getInt(), receiveData.getInt()))
                .collect(Collectors.toList());
    }

    public static int packetResponseSize() {
        return 1000;
    }
}
