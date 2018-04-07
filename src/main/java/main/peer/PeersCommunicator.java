package main.peer;

import main.TorrentInfo;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PeersCommunicator {
    private Peer me;
    private Peer peer;
    private Socket peerSocket;
    private TorrentInfo torrentInfo;

    private SendPeerMessages sendMessages;
    private ReceiveMessages receiveMessages;
    private SpeedStatistics peerSpeedStatistics;

    public PeersCommunicator(TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                             DataInputStream dataInputStream,
                             DataOutputStream peerDataOutputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.torrentInfo = torrentInfo;
        this.me = new Peer("localhost", peerSocket.getLocalPort());

        this.sendMessages = new SendPeerMessagesImpl(this.me, this.peer, this::closeConnection,
                peerDataOutputStream);
        this.receiveMessages = new ReceivedMessagesImpl(this.me, this.peer, dataInputStream);

        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                this.receiveMessages.getPeerMessageResponseFlux(),
                this.sendMessages.sentPeerMessagesFlux());
    }

    public ReceiveMessages receivePeerMessages() {
        return receiveMessages;
    }

    public Peer getPeer() {
        return peer;
    }

    public Peer getMe() {
        return me;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public void closeConnection() {
        try {
            this.peerSocket.close();
        } catch (IOException exception) {
            // TODO: do something better... it's a fatal problem with my design!!!
            exception.printStackTrace();
        }
    }

    public SpeedStatistics getPeerSpeedStatistics() {
        return peerSpeedStatistics;
    }

    @Override
    public String toString() {
        return "PeersCommunicator{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }

    public SendPeerMessages sendMessages() {
        return sendMessages;
    }
}
