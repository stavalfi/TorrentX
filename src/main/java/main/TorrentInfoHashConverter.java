package main;

public class TorrentInfoHashConverter {
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
    public static byte[] torrentInfoHashToBytes(String torrentInfoHash)
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
//                System.out.println(torrentInfoHash);
//                System.out.println( Hex.encodeHexString( bytes ) );
        return bytes;
    }
    public static String bytesToTorrentInfoHash(byte[] bytes)
    {
        return new String(bytes);
    }
}
