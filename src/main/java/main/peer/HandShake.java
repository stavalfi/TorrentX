package main.peer;

import lombok.Getter;
import lombok.Setter;
import main.HexByteConverter;
import org.joou.UByte;

import java.nio.ByteBuffer;

import static org.joou.Unsigned.ubyte;

@Getter
@Setter
public class HandShake {
    private UByte pstrLength;
    // pstr - string identifier of the protocol
    private byte[] pstr;
    // reserved- eight (8) reserved bytes.
    // All current implementations use all zeroes.
    // Each bit in these bytes can be used to change
    // the behavior of the protocol.
    private byte[] reserved;
    private final byte[] torrentInfoHash;// 20 bytes
    private final byte[] peerId; // 20 bytes

    public HandShake(byte[] torrentInfoHash, byte[] peerId) {
        int reservedBytesAmount = 8;
        String protocolVersion = "BitTorrent protocol";
        assert protocolVersion.length() == 19;
        assert torrentInfoHash.length == 20;
        assert peerId.length == 20;

        // pstr.length() return the number of chars in it without the "/n".
        this.pstrLength = ubyte(protocolVersion.length());
        this.pstr = protocolVersion.getBytes();
        //this.reserved = ByteBuffer.wrap(new byte[reservedBytesAmount]);
        this.reserved = HexByteConverter.hexToByte("8000000000130004");

        this.torrentInfoHash = torrentInfoHash;
        this.peerId = peerId;
        assert reserved.length == reservedBytesAmount;
    }

    @Override
    public String toString() {
        String peerId = new String(this.peerId);
        return "torrentInfoHash: " + HexByteConverter.byteToHex(this.torrentInfoHash) + " , peerId: " + peerId;
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
        ByteBuffer buffer = ByteBuffer.allocate(49 + handShake.pstrLength.intValue());
        byte b = 19;

        assert buffer.capacity() == 68;
        assert handShake.pstrLength.intValue() == 19;
        assert handShake.pstr.length == 19;
        assert handShake.reserved.length == 8;
        assert handShake.getTorrentInfoHash().length == 20;
        assert handShake.peerId.length == 20;

        buffer.put(b);
        buffer.put(handShake.pstr);
        buffer.put(handShake.reserved);
        buffer.put(handShake.getTorrentInfoHash());
        buffer.put(handShake.peerId);

        return buffer.array();
    }

    public static HandShake createObjectFromPacket(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        UByte pstrLength = ubyte(buffer.get());

        byte[] pstrByteArray = new byte[pstrLength.intValue()];
        buffer.get(pstrByteArray);

        byte[] reservedByteArray = new byte[8];
        buffer.get(reservedByteArray);

        byte[] torrentInfoHashByteArray = new byte[20];
        buffer.get(torrentInfoHashByteArray);

        byte[] peerIdByteArray = new byte[20];
        buffer.get(peerIdByteArray);

        HandShake handShake = new HandShake(torrentInfoHashByteArray, peerIdByteArray);
        handShake.setPstrLength(pstrLength);
        handShake.setPstr(pstrByteArray);
        handShake.setReserved(reservedByteArray);

        return handShake;
    }

}
