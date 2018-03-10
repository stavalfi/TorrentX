package main.tracker.requests;

import main.tracker.Tracker;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class AnnounceRequest extends TrackerRequest {

    private final long connectionId;
    private final byte[] torrentInfoHash;
    private final byte[] peerId;
    private final long downloaded;
    private final long left;
    private final long uploaded;
    private final int event;
    private final int ipAddress;
    private final int key;
    private final int numWant;
    private final short tcpPort;

    public AnnounceRequest(Tracker tracker, long connectionId,
                           byte[] torrentInfoHash, byte[] peerId, int numWant, short tcpPort) {
        this(tracker, connectionId, 123456,
                torrentInfoHash, peerId, 0, 0,
                0, 0, 0, 0,
                numWant, tcpPort);
    }


    public AnnounceRequest(Tracker tracker, long connectionId, int transactionId,
                           byte[] torrentInfoHash, byte[] peerId, long downloaded,
                           long left, long uploaded, int event, int ipAddress,
                           int key, int numWant, short tcpPort) {
        super(tracker, 1, transactionId);
        this.connectionId = connectionId;
        this.torrentInfoHash = torrentInfoHash;
        this.peerId = peerId;
        this.downloaded = downloaded;
        this.left = left;
        this.uploaded = uploaded;
        this.event = event;
        this.ipAddress = ipAddress;
        this.key = key;
        this.numWant = numWant;
        this.tcpPort = tcpPort;
    }

    /**
     * offset == bytes not bits!!!!!!
     * Offset  Size    Name    Value
     * 0       64-bit integer  connection_id    same connection_id // the connectionId we received from the server after we successfully connected
     * 8       32-bit integer  action          1                   // scrapeMono
     * 12      32-bit integer  transaction_id                      // we randomly decide
     * 16      20-byte string  info_hash  torrent_info_hash // the hash of the torrent we want to scrapeMono on
     * 36      20-byte string  peer_id  my-peer-id-!??!!?! how do I get it omg?
     * 56      64-bit integer  downloaded 0? // The number of byte you've downloaded in this session.
     * 64      64-bit integer  left     0? //The number of bytes you have left to download until you're finished.
     * 72      64-bit integer  uploaded 0? // The number of bytes you have uploaded in this session.
     * 80      32-bit integer  event           0                   // 0: none; 1: completed; 2: started; 3: stopped
     * 84      32-bit integer  IP address      0                   // 	Your ip address. Set to 0 if you want the tracker to use the sender of this udp packet.
     * 88      32-bit integer  key            ????? // A unique key that is randomized by the client.
     * 92      32-bit integer  num_want        10                  // The maximum number of peers you want in the reply. Use -1 for default.
     * 96      16-bit integer  tcpPort       ???? // The tcpPort you're listening on.
     * 98
     */
    @Override
    public ByteBuffer buildRequestPacket() {

        ByteBuffer sendData = ByteBuffer.allocate(98); // we need 98 bits at list
        sendData.putLong(this.connectionId); // connection_id
        sendData.putInt(getActionNumber()); // action we want to perform - scrapeMono
        sendData.putInt(getTransactionId()); // transaction_id - random int we make (32 bits)
        sendData.put(this.torrentInfoHash); //info_hash (20 bits)
        sendData.put(this.peerId); // peer_id (20 bits)
        sendData.putLong(this.downloaded); // downloaded (64 bits)
        sendData.putLong(this.left); // left (64 bits)
        sendData.putLong(this.uploaded); // uploaded (64 bits)
        sendData.putInt(this.event); // event = 0 , options: 0: none; 1: completed; 2: started; 3: stopped
        sendData.putInt(this.ipAddress); // IP address = 0 = default = the ip of the sender of this packet = me
        sendData.putInt(this.key); // key  (32 bits)
        sendData.putInt(this.numWant); // num_want = The maximum number of peers you want in the reply. Use -1 for default.
        sendData.putShort(this.tcpPort); // tcpPort (16 bits)

        return sendData;
    }

    @Override
    public String toString() {
        return "AnnounceRequest{" +
                "connectionId=" + connectionId +
                ", torrentInfoHash=" + Arrays.toString(torrentInfoHash) +
                ", peerId=" + Arrays.toString(peerId) +
                ", downloaded=" + downloaded +
                ", left=" + left +
                ", uploaded=" + uploaded +
                ", event=" + event +
                ", ipAddress=" + ipAddress +
                ", key=" + key +
                ", numWant=" + numWant +
                ", tcpPort=" + tcpPort +
                '}' + super.toString();
    }

    public long getConnectionId() {
        return connectionId;
    }

    public byte[] getTorrentInfoHash() {
        return torrentInfoHash;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getLeft() {
        return left;
    }

    public long getUploaded() {
        return uploaded;
    }

    public int getEvent() {
        return event;
    }

    public int getIpAddress() {
        return ipAddress;
    }

    public int getKey() {
        return key;
    }

    public int getNumWant() {
        return numWant;
    }

    public short getTcpPort() {
        return tcpPort;
    }
}
