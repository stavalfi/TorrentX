package main.requests;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@ToString
public class ScrapeRequest implements PacketRequest
{
    private final long connectionId;
    private final int action=2;
    private final int transactionId=123456;
    private final String torrentInfoHash;

    public ScrapeRequest(long connectionId,String torrentInfoHash)
    {
        this.connectionId=connectionId;
        this.torrentInfoHash=torrentInfoHash;
    }

    /** offset == bytes not bits!!!!!!
     * Offset          Size            Name            Value
     * 0               64-bit integer  connection_id   connectionId
     * 8               32-bit integer  action          2                // scrape
     * 12              32-bit integer  transaction_id we-random-numbers
     * 16 + 20 * n     20-byte string  torrentInfoHash  torrent_info_hash // the hash of the torrent we want to scrape on
     * 16 + 20 * N
     */
    public byte[] buildRequestPacket() {

        ByteBuffer sendData = ByteBuffer.allocate(36);
        sendData.putLong(this.connectionId); // connection_id (64 bit)
        sendData.putInt(this.action); // action we want to perform - scrape the server (32 bits)
        sendData.putInt(this.transactionId); // transaction_id - random int we make (32 bits)
        sendData.put(AnnounceRequest.castTorrentInfoHash(this.torrentInfoHash)); // torrentInfoHash = (20 bits)

        return sendData.array();
    }
}