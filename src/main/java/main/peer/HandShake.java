package main.peer;

import lombok.Getter;
import main.TorrentInfoHashConverter;

import java.nio.ByteBuffer;

public class HandShake {
    private final byte pstrLength;
    @Getter private final ByteBuffer pstr;
    @Getter private final ByteBuffer reserved;
    @Getter private final String torrentInfoHash;// 20 bytes
    @Getter private final String peerId; // 20 bytes

    public HandShake(ByteBuffer reserved,
                     String torrentInfoHash, String peerId) {
        this((byte)0x08,ByteBuffer.allocate(8), reserved, torrentInfoHash, peerId);
    }

    public HandShake(byte pstrLength,ByteBuffer pstr, ByteBuffer reserved,
                     String torrentInfoHash, String peerId) {
        this.pstrLength=pstrLength;
        this.pstr = ByteBuffer.wrap(pstr.array());
        this.reserved = ByteBuffer.wrap(reserved.array());
        this.torrentInfoHash = torrentInfoHash;
        this.peerId = peerId;
        assert reserved.capacity() == 8;
    }

    public int getPstrLength() {
        return this.pstrLength;
    }

    /**
     * cast HandShake object to HandShake packet in bytes.
     *
     * @param handShake object
     * @return a byte array (49 + pstrlen bytes) with the following structure:
     * Offset       Size            Name        value
     * 0            8-bit           byte        pstrLength
     * 1            pstrlen-bit     bytes       pstr
     * 1+pstrlen    8-bit           byte        reserved
     * 9+pstrlen    20-bit          String      torrentInfoHash
     * 29+pstrlen   20-bit          String      peerId
     * 49+pstrlen
     */
    public static byte[] createPacketFromObject(HandShake handShake) {
        ByteBuffer buffer = ByteBuffer.allocate(49+handShake.pstrLength);

        buffer.put(handShake.pstrLength);
        buffer.put(handShake.pstr);
        buffer.put(handShake.reserved);
        buffer.put(TorrentInfoHashConverter.torrentInfoHashToBytes(handShake.torrentInfoHash));
        buffer.put(handShake.peerId.getBytes());

        return buffer.array();
    }

    public static HandShake createObjectFromPacket(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);

        byte pstrLength=buffer.get();
        ByteBuffer pstr=buffer.get(new byte[pstrLength]);
        ByteBuffer reserved=buffer.get(new byte[8]);
        String torrentInfoHash=TorrentInfoHashConverter.bytesToTorrentInfoHash(buffer.get(new byte[20]).array());
        String peerId=new String(buffer.get(new byte[20]).array());

        return new HandShake(pstrLength,pstr,reserved,torrentInfoHash,peerId);
    }

}
