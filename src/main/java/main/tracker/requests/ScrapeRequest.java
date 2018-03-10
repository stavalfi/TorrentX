package main.tracker.requests;

import main.tracker.Tracker;

import java.nio.ByteBuffer;
import java.util.List;

public class ScrapeRequest extends TrackerRequest {
    private final long connectionId;
    private final List<byte[]> torrentInfoHashes;

    public ScrapeRequest(Tracker tracker, long connectionId, int transactionId, List<byte[]> torrentInfoHashes) {
        super(tracker,2, transactionId);
        this.connectionId = connectionId;
        this.torrentInfoHashes = torrentInfoHashes;
    }

    /**
     * offset == bytes not bits!!!!!!
     * Offset          Size            Name            Value
     * 0               64-bit integer  connection_id   connectionId
     * 8               32-bit integer  action          2                // scrapeMono
     * 12              32-bit integer  transaction_id we-random-numbers
     * 16 + 20 * n     20-byte string  torrentInfoHash  torrent_info_hash // the hash of the torrent we want to scrapeMono on
     * 16 + 20 * N
     */
    @Override
    public ByteBuffer buildRequestPacket() {

        ByteBuffer sendData = ByteBuffer.allocate(36);
        sendData.putLong(this.connectionId); // connection_id (64 bit)
        sendData.putInt(getActionNumber()); // action we want to perform - scrapeMono the server (32 bits)
        sendData.putInt(getTransactionId()); // transaction_id - random int we make (32 bits)
        // each torrentInfoHash byte array is 20 bytes.
        this.torrentInfoHashes.forEach(sendData::put);

        return sendData;
    }

    @Override
    public String toString() {
        return "ScrapeRequest{" +
                "connectionId=" + connectionId +
                ", torrentInfoHashes=" + torrentInfoHashes +
                "} " + super.toString();
    }
}