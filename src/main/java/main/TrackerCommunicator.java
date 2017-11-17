package main;

import main.requests.*;
import main.response.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * https://web.archive.org/web/20170101194115/http://bittorrent.org/beps/bep_0015.html
 * http://www.rasterbar.com/products/libtorrent/udp_tracker_protocol.html
 */
public class TrackerCommunicator {

    public static ConnectionResponse communicate(String trackerIp, int trackerUdpPort,
                                                 ConnectionRequest connectionRequest) throws IOException {
        byte[] response = new byte[ConnectionResponse.packetResponseSize()];

        communicate(trackerIp, trackerUdpPort, connectionRequest.buildRequestPacket(), response);

        return new ConnectionResponse(response);
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
    public static AnnounceResponse communicate(String trackerIp, int trackerUdpPort, AnnounceRequest announceRequest) throws IOException {
        byte[] response = new byte[AnnounceResponse.packetResponseSize()];

        communicate(trackerIp, trackerUdpPort, announceRequest.buildRequestPacket(), response);

        // NumWant == how much peers's ip&port we asked for.
        return new AnnounceResponse(response, announceRequest.getNumWant());
    }

    public static ScrapeResponse communicate(String trackerIp, int trackerUdpPort, ScrapeRequest scrapeRequest) throws IOException {
        byte[] response = new byte[ScrapeResponse.packetResponseSize()];

        communicate(trackerIp, trackerUdpPort, scrapeRequest.buildRequestPacket(), response);

        return new ScrapeResponse(response, scrapeRequest.getTorrentInfoHashs());
    }
}
