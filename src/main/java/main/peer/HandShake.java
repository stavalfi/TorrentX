package main.peer;

import lombok.Getter;
import lombok.Setter;
import main.NaturalX.Natural1;
import main.TorrentInfoHashConverter;

import java.nio.ByteBuffer;

@Getter
@Setter
public class HandShake {
    private Natural1 pstrLength;
    // pstr - string identifier of the protocol
    private ByteBuffer pstr;
    // reserved- eight (8) reserved bytes.
    // All current implementations use all zeroes.
    // Each bit in these bytes can be used to change
    // the behavior of the protocol.
    private ByteBuffer reserved;
    private final byte[] torrentInfoHash;// 20 bytes
    private final String peerId; // 20 bytes

    public HandShake(String torrentInfoHash, String peerId) {
        int reservedBytesAmount = 8;
        String protocolVersion = "BitTorrent protocol";

        assert torrentInfoHash.length() == 40;
        assert protocolVersion.length() == 19;
        assert peerId.length() == 20;

        // pstr.length() return the number of chars in it without the "/n".
        this.pstrLength = new Natural1(protocolVersion.length());
        this.pstr = ByteBuffer.wrap(protocolVersion.getBytes());
        this.reserved = ByteBuffer.wrap(new byte[reservedBytesAmount]);
        this.torrentInfoHash = TorrentInfoHashConverter.torrentInfoHashToBytes(torrentInfoHash);
        this.peerId = peerId;
        assert reserved.capacity() == reservedBytesAmount;
    }

    public int getPstrLength() {
        return (int) this.pstrLength.getNumber();
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
     * 1+pstrlen    8-bit           byte        reserved
     * 9+pstrlen    20-bit          String      torrentInfoHash
     * 29+pstrlen   20-bit          String      peerId
     * 49+pstrlen
     */
    public static byte[] createPacketFromObject(HandShake handShake) {
        ByteBuffer buffer = ByteBuffer.allocate(49 + (int) handShake.pstrLength.getNumber());
        buffer.put(handShake.pstrLength.buffer());
        buffer.put(handShake.pstr);
        buffer.put(handShake.reserved);
        buffer.put(handShake.getTorrentInfoHash());
        buffer.put(handShake.peerId.getBytes());

        return buffer.array();
    }

    public static HandShake createObjectFromPacket(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        Natural1 pstrLength = new Natural1(buffer.get());
        ByteBuffer pstr = buffer.get(new byte[(int) pstrLength.getNumber()]);
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
