package main.peer;

import lombok.Getter;
import lombok.Setter;
import main.TorrentInfoHashConverter;
import org.joou.UByte;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.joou.Unsigned.ubyte;

@Getter
@Setter
public class HandShake {
    private UByte pstrLength;
    // pstr - string identifier of the protocol
    private ByteBuffer pstr;
    // reserved- eight (8) reserved bytes.
    // All current implementations use all zeroes.
    // Each bit in these bytes can be used to change
    // the behavior of the protocol.
    private ByteBuffer reserved;
    private final byte[] torrentInfoHash;// 20 bytes
    private final String peerId; // 20 bytes

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public HandShake(String torrentInfoHash, String peerId) {
        int reservedBytesAmount = 8;
        String protocolVersion = "BitTorrent protocol";

        assert torrentInfoHash.length() == 40;
        assert protocolVersion.length() == 19;
        assert peerId.length() == 20;

        // pstr.length() return the number of chars in it without the "/n".
        this.pstrLength = ubyte(protocolVersion.length());
        this.pstr = ByteBuffer.wrap(protocolVersion.getBytes());
        //this.reserved = ByteBuffer.wrap(new byte[reservedBytesAmount]);
        this.reserved = ByteBuffer.wrap(hexStringToByteArray("8000000000130004"));

        this.torrentInfoHash = TorrentInfoHashConverter.torrentInfoHashToBytes(torrentInfoHash);
        this.peerId = peerId;
        assert reserved.capacity() == reservedBytesAmount;
    }



    /**
     * cast HandShake object to HandShake packet in bytes.
     *
     * @param handShake object
     * @return a byte buffer (49 + pstrlen bytes) with the following structure:
     * <pstrlen><pstr><reserved><info_hash><peer_id>
     * Offset       Size            Name        value
     * <p>
     * 0            8-bit           byte        pstrLength
     * 1            pstrlen-bit     bytes       pstr
     * 1+pstrlen    64-bit           byte        reserved
     * 9+pstrlen    20-bit          String      torrentInfoHash
     * 29+pstrlen   20-bit          String      peerId
     * 49+pstrlen
     */
    public static byte[] createPacketFromObject(HandShake handShake) {
        ByteBuffer buffer = ByteBuffer.allocate(49 + (int) handShake.pstrLength.intValue());
        assert buffer.capacity() == 68;
        assert handShake.pstrLength.intValue() == 19;
        byte b=19;
        buffer.put(b);
        buffer.put(handShake.pstr);
        assert handShake.pstr.array().length == 19;
        buffer.put(handShake.reserved);
        System.out.println(handShake.reserved.capacity());
        assert handShake.reserved.capacity() == 8;

        buffer.put(handShake.getTorrentInfoHash());
        assert handShake.getTorrentInfoHash().length == 20;

        buffer.put(handShake.peerId.getBytes());
        assert handShake.peerId.getBytes().length == 20;


        return buffer.array();
    }

    public static HandShake createObjectFromPacket(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        UByte pstrLength = ubyte(buffer.get());
        ByteBuffer pstr = buffer.get(new byte[pstrLength.intValue()]);
        ByteBuffer reserved = buffer.get(new byte[8]);
        String torrentInfoHash = TorrentInfoHashConverter.bytesToTorrentInfoHash(buffer.get(new byte[20]).array());

        String peerId = new String(buffer.get(new byte[20]).array());

        HandShake handShake = new HandShake(torrentInfoHash, peerId);
        handShake.setPstrLength(pstrLength);
        handShake.setPstr(pstr);
        handShake.setReserved(reserved);

        return handShake;
    }

}
