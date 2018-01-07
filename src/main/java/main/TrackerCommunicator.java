package main;

import lombok.SneakyThrows;
import main.tracker.requests.AnnounceRequest;
import main.tracker.requests.ConnectRequest;
import main.tracker.requests.ScrapeRequest;
import main.tracker.response.AnnounceResponse;
import main.tracker.response.ConnectResponse;
import main.tracker.response.ScrapeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * https://web.archive.org/web/20170101194115/http://bittorrent.org/beps/bep_0015.html
 * http://www.rasterbar.com/products/libtorrent/udp_tracker_protocol.html
 */
public class TrackerCommunicator {
    private static Logger logger = LoggerFactory.getLogger(TrackerCommunicator.class);

    @SneakyThrows
    public static ConnectResponse communicate(String trackerIp, int trackerUdpPort,
                                              ConnectRequest connectRequest) {
        logger.info("sending connect request to tracker: " + trackerIp + ":" + trackerUdpPort);

        byte[] response = new byte[ConnectResponse.packetResponseSize()];

        communicate(trackerIp, trackerUdpPort, connectRequest.buildRequestPacket(), response);
        ConnectResponse connectResponse = new ConnectResponse(response);

        logger.info("receive connect response from tracker: " + trackerIp + ":" + trackerUdpPort);
        return connectResponse;
    }

    @SneakyThrows
    public static AnnounceResponse communicate(String trackerIp, int trackerUdpPort, AnnounceRequest announceRequest) {
        logger.info("sending announce request to tracker: " + trackerIp + ":" + trackerUdpPort);

        byte[] response = new byte[AnnounceResponse.packetResponseSize()];
        communicate(trackerIp, trackerUdpPort, announceRequest.buildRequestPacket(), response);
        // NumWant == how much peers's ip&port we asked for.
        AnnounceResponse announceResponse = new AnnounceResponse(response, announceRequest.getNumWant());

        logger.info("receive announce response from tracker: " + trackerIp + ":" + trackerUdpPort);
        return announceResponse;
    }

    public static ScrapeResponse communicate(String trackerIp, int trackerUdpPort, ScrapeRequest scrapeRequest) throws IOException {
        logger.info("sending scrape request to tracker: " + trackerIp + ":" + trackerUdpPort);
        byte[] response = new byte[ScrapeResponse.packetResponseSize()];

        communicate(trackerIp, trackerUdpPort, scrapeRequest.buildRequestPacket(), response);

        ScrapeResponse scrapeResponse = new ScrapeResponse(response, scrapeRequest.getTorrentInfoHashs());
        logger.info("receive scrape response from tracker: " + trackerIp + ":" + trackerUdpPort);
        return scrapeResponse;
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
