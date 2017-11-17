package main.requests;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Hex;

import javax.xml.bind.DatatypeConverter;

@Getter
@ToString
public class AnnounceRequest implements PacketRequest {

    private final long connectionId;
    private final int action=1;
    private final int transactionId=123456;
    private final String torrentInfoHash;
    private final String peerId;
    private final long downloaded;
    private final long left;
    private final long uploaded;
    private final int event;
    private final int ipAddress;
    private final int key;
    private final int numWant;
    private final short tcpPort;


    public AnnounceRequest(long connectionId,String torrentInfoHash,final String peerId,final long downloaded
            ,long left,long uploaded,int event,int ipAddress,int key,int numWant,short tcpPort)
    {
        this.connectionId=connectionId;
        this.torrentInfoHash=torrentInfoHash;
        this.peerId=peerId;
        this.downloaded=downloaded;
        this.left=left;
        this.uploaded=uploaded;
        this.event=event;
        this.ipAddress=ipAddress;
        this.key=key;
        this.numWant=numWant;
        this.tcpPort = tcpPort;
    }

    /** offset == bytes not bits!!!!!!
     * Offset  Size    Name    Value
     * 0       64-bit integer  connection_id    same connection_id // the connectionId we received from the server after we successfully connected
     * 8       32-bit integer  action          1                   // announce
     * 12      32-bit integer  transaction_id                      // we randomly decide
     * 16      20-byte string  info_hash  torrent_info_hash // the hash of the torrent we want to scrape on
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
    public byte[] buildRequestPacket() {

        ByteBuffer sendData = ByteBuffer.allocate(98); // we need 98 bits at list
        sendData.putLong(this.connectionId); // connection_id
        sendData.putInt(this.action); // action we want to perform - announce
        sendData.putInt(this.transactionId); // transaction_id - random int we make (32 bits)
        sendData.put(castTorrentInfoHash(this.torrentInfoHash)); //info_hash (20 bits)
        sendData.put(new byte[20]); // peer_id (20 bits)
        sendData.putLong(this.downloaded); // downloaded (64 bits)
        sendData.putLong(this.left); // left (64 bits)
        sendData.putLong(this.uploaded); // uploaded (64 bits)
        sendData.putInt(this.event); // event = 0 , options: 0: none; 1: completed; 2: started; 3: stopped
        sendData.putInt(this.ipAddress); // IP address = 0 = default = the ip of the sender of this packet = me
        sendData.putInt(this.key); // key  (32 bits)
        sendData.putInt(this.numWant); // num_want = The maximum number of peers you want in the reply. Use -1 for default.
        sendData.putShort(this.tcpPort); // tcpPort (16 bits)

        return sendData.array();
    }

    /**
     * torrentInfoHash is a representation of hex. every pair of hex is one byte!
     * for example:
     * 99FEAE0A05C6A5DD9AF939FFCE5CA9B0D16F31B0 (which is 40 bytes if we cast every char to bit) , but it is actually:
     * 0x99 0xFE 0xAE 0x0A 0x05 0xC6 0xA5 0xDD 0x9A 0xF9 0x39 0xFF 0xCE 0x5C 0xA9 0xB0 0xD1 0x6F 0x31 0xB0
     * @return 20 bytes array. in the example, it should return the byte representation of the following hex array:
     * 0x99 0xFE 0xAE 0x0A 0x05 0xC6 0xA5 0xDD 0x9A 0xF9 0x39 0xFF 0xCE 0x5C 0xA9 0xB0 0xD1 0x6F 0x31 0xB0
     *
     * Note: I have no idea why this method work and I really don't care. but it is doing but it should ;D
     */
    static byte[] castTorrentInfoHash(String torrentInfoHash)
    {
        byte[] bytes = new byte[20];

        for (int i = 0; i < torrentInfoHash.length(); i += 2) {

            byte left = (byte)Character.digit(torrentInfoHash.charAt(i),16);
            // System.out.println(Integer.toBinaryString(left & 0xff));
            left = (byte)(left << 4);
            // System.out.println(Integer.toBinaryString(left & 0xff));

            byte right = (byte)Character.digit(torrentInfoHash.charAt(i+1),16);
            // incase of printing:
            // toBinaryString expect integer and not byte so it implicitly cast the variable
            // `right` with `sign-extended`. for example:
            // 00001111 ->(cast to int) 00000000000000000000000000001111 -> print 00001111 (why?)
            // 10001111 ->(cast to int) 11111111111111111111111110001111 -> print 11111111111111111111111110001111
            // so if we do: 11111111111111111111111110001111 & 0xff == 11111111111111111111111110001111 & 0x000000ff == 0x0f == 10001111
            // -> I have no idea why :(
            // System.out.println(Integer.toBinaryString(right & 0xff));

            bytes[i/2] = (byte)(left | right);
            // System.out.println(Integer.toBinaryString(torrentInfoHash[i/2] & 0xff));
        }
        //        System.out.println(torrentInfoHash);
        //        System.out.println( Hex.encodeHexString( bytes ) );
        return bytes;
    }
}
