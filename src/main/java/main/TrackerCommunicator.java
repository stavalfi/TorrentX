package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * https://web.archive.org/web/20170101194115/http://bittorrent.org/beps/bep_0015.html
 */
public class TrackerCommunicator {

    /**
     * create scrape packet with default settings.
     *
     * @param connectionId we retrieve from the server after we successfully connected
     * @return
     */
    private static byte[] buildDefaultScrapePacket(long connectionId) {
        /** offset == bytes not bits!!!!!!
         * Offset          Size            Name            Value
         * 0               64-bit integer  connection_id
         * 8               32-bit integer  action          2                // scrape
         * 12              32-bit integer  transaction_id
         * 16 + 20 * n     20-byte string  info_hash
         * 16 + 20 * N
         */

        ByteBuffer sendData = ByteBuffer.allocate(36);
        sendData.putLong(connectionId); // connection_id (64 bit)
        sendData.putInt(2); // action we want to perform - scrape the server (32 bits)
        sendData.putInt(123456); // transaction_id - random int we make (32 bits)
        //        sendData.put(); // info_hash = (20 bits)

        return sendData.array();
    }

    /**
     * create connection packet with default settings.
     *
     * @return the packet
     */
    private static byte[] buildDefaultConnectionPacket() {
        /** offset == bytes not bits!!!!!!
         * Offset  Size            Name            Value
         * 0       64-bit integer  connection_id   0x41727101980
         * 8       32-bit integer  action          0 // connect
         * 12      32-bit integer  transaction_id  random (we decide)
         * 16
         */

        ByteBuffer sendData = ByteBuffer.allocate(128);
        sendData.putLong(0x41727101980L); // connection_id - can't change (64 bits)
        sendData.putInt(0); // action we want to perform - connecting with the server (32 bits)
        sendData.putInt(123456); // transaction_id - random int we make (32 bits)

        return sendData.array();
    }

    /**
     * create announce packet with default settings.
     *
     * @param connectionId we retrieve from the server after we successfully connected
     * @return the packet
     */
      private static byte[] buildDefaultAnnouncePacket(long connectionId) {
         /** offset == bytes not bits!!!!!!
         * Offset  Size    Name    Value
         * 0       64-bit integer  connection_id    same connection_id // the connectionId we received from the server after we successfully connected
         * 8       32-bit integer  action          1                   // announce
         * 12      32-bit integer  transaction_id                      // we randomly decide
         * 16      20-byte string  info_hash
         * 36      20-byte string  peer_id
         * 56      64-bit integer  downloaded
         * 64      64-bit integer  left
         * 72      64-bit integer  uploaded
         * 80      32-bit integer  event           0                   // 0: none; 1: completed; 2: started; 3: stopped
         * 84      32-bit integer  IP address      0                   // default
         * 88      32-bit integer  key
         * 92      32-bit integer  num_want        -1                  // default
         * 96      16-bit integer  port
         * 98
         */

        ByteBuffer sendData = ByteBuffer.allocate(98); // we need 98 bits at list
        sendData.putLong(connectionId); // connection_id
        sendData.putInt(1); // action we want to perform - announce
        sendData.putInt(123456); // transaction_id - random int we make (32 bits)
        //        sendData.put(); //info_hash (20 bits)
        //        sendData.put(); // peer_id (20 bits)
        //        sendData.put(); // downloaded (64 bits)
        //        sendData.put(); // left (64 bits)
        //        sendData.put(); // uploaded (64 bits)
        sendData.putInt(80, 0); // event = 0 , options: 0: none; 1: completed; 2: started; 3: stopped
        sendData.putInt(0); // IP address = 0 = default
        //        sendData.put(); // key  (32 bits)
        sendData.putInt(92, -1); // num_want = -1 = default
        //        sendData.put(); // port (16 bits)

        return sendData.array();
    }

    /**
     * check the status of the tracker and retreive the most up to date seeders and leechers amount.
     *
     * @param ip           or url of the tracker
     * @param port         of the tracker
     * @param connectionId we retrieve from the server after we successfully connected.
     */
    public static void scrape(String ip, int port, long connectionId) throws IOException {
        byte[] receiveData = new byte[1024]; // we need to receive 128 bytes but we may get more so let's assume we get more..
        communicate(ip, port, buildDefaultScrapePacket(connectionId), receiveData);

        // test we successfully scraped

        /**
         * Offset      Size            Name            Value
         * 0           32-bit integer  action          2 // scrape
         * 4           32-bit integer  transaction_id
         * 8 + 12 * n  32-bit integer  seeders
         * 12 + 12 * n 32-bit integer  completed
         * 16 + 12 * n 32-bit integer  leechers
         * 8 + 12 * N
         */

        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        System.out.println("To SERVER:");
        System.out.println("action id: " + 2);
        System.out.println("transaction_id: " + 123456);
        System.out.println("FROM SERVER:");
        System.out.println("action id: " + receiveData_analyze.getInt());
        System.out.println("transaction_id: " + receiveData_analyze.getInt());
        System.out.println("seeders: " + receiveData_analyze.getInt());
        System.out.println("completed: " + receiveData_analyze.getInt());
        System.out.println("leechers: " + receiveData_analyze.getInt());
    }

    /**
     * try to announce the server we start downloading == we become a seeder.
     *
     * @param ip           or url of the tracker
     * @param port         of the tracker
     * @param connectionId we retrieve from the server after we successfully connected.
     * @throws IOException
     */
    public static void announce(String ip, int port, long connectionId) throws IOException {
        byte[] receiveData = new byte[1024]; // we need to receive 128 bytes but we may get more so let's assume we get more..
        communicate(ip, port, buildDefaultAnnouncePacket(connectionId), receiveData);

        // test we successfully announced

        /**
         * Offset      Size            Name            Value
         * 0           32-bit integer  action          1 // announce
         * 4           32-bit integer  transaction_id
         * 8           32-bit integer  interval
         * 12          32-bit integer  leechers
         * 16          32-bit integer  seeders
         * 20 + 6 * n  32-bit integer  IP address
         * 24 + 6 * n  16-bit integer  TCP port
         * 20 + 6 * N
         */
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        System.out.println("To SERVER:");
        System.out.println("action id: " + 1);
        System.out.println("transaction_id: " + 123456);
        System.out.println("FROM SERVER:");
        System.out.println("action id: " + receiveData_analyze.getInt());
        System.out.println("transaction_id: " + receiveData_analyze.getInt());
        System.out.println("interval: " + receiveData_analyze.getInt());
        System.out.println("leechers: " + receiveData_analyze.getInt());
        System.out.println("seeders: " + receiveData_analyze.getInt());
        System.out.println("IP address: " + receiveData_analyze.getInt());
        System.out.println("TCP port: " + receiveData_analyze.getShort());
    }


    /**
     * Try to connect to a server
     *
     * @param ip   or url of the tracker
     * @param port of the tracker
     * @throws IOException
     */
    public static long connect(String ip, int port) throws IOException {
        byte[] receiveData = new byte[1024]; // we need to receive 128 bytes but we may get more so let's assume we get more..
        communicate(ip, port, buildDefaultConnectionPacket(), receiveData);


        /** test we successfully connected
         * The server must return us a UDP packet with the following structure:
         *
         * Offset  Size            Name            Value
         * 0       32-bit integer  action          0 // connect
         * 4       32-bit integer  transaction_id
         * 8       64-bit integer  connection_id
         * 16
         */

        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        System.out.println("To SERVER:");
        System.out.println("action id: " + 0);
        System.out.println("transaction_id: " + 123456);
        System.out.println("connection_id from server: " + 0x41727101980L);
        System.out.println("FROM SERVER:");
        System.out.println("action id: " + receiveData_analyze.getInt());
        System.out.println("transaction_id: " + receiveData_analyze.getInt());
        long connectionId = receiveData_analyze.getLong();
        System.out.println("connection_id from server: " + connectionId);

        return connectionId;
    }

    private static void communicate(String ip, int port, byte[] sendData, byte[] receiveData) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(ip);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        clientSocket.close();
    }
}
