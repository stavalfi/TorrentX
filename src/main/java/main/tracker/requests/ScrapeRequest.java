package main.tracker.requests;

import lombok.Getter;
import lombok.ToString;
import main.HexByteConverter;

import java.nio.ByteBuffer;
import java.util.List;

@Getter
@ToString
public class ScrapeRequest implements PacketRequest
{
    private final long connectionId;
    private final int action=2;
    private final int transactionId=123456;
    private final List<String> torrentInfoHashs;

    public ScrapeRequest(long connectionId,final List<String> torrentInfoHashs)
    {
        this.connectionId=connectionId;
        this.torrentInfoHashs=torrentInfoHashs;
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
        // each torrentInfoHash byte array is 20 bytes.
        this.torrentInfoHashs.forEach((String torrentInfoHash)->sendData.put(HexByteConverter.hexToByte(torrentInfoHash)));

        return sendData.array();
    }
}