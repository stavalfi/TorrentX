package main.peer;

import main.TorrentInfo;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Link {
    private Peer me;
    private Peer peer;
    private Socket peerSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private TorrentInfo torrentInfo;
    private PeerCurrentStatus peerCurrentStatus;
    private SendMessagesNotifications sendMessages;
    private ReceiveMessagesNotifications receiveMessagesNotifications;
    private SpeedStatistics peerSpeedStatistics;

    public Link(Link link) {
        this(link.torrentInfo, link.peer, link.peerSocket,
                link.dataInputStream, link.dataOutputStream);
    }

    public Link(TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                DataInputStream dataInputStream,
                DataOutputStream dataOutputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.torrentInfo = torrentInfo;
        this.me = new Peer("localhost", peerSocket.getLocalPort());

        this.peerCurrentStatus = new PeerCurrentStatus(torrentInfo.getPieces().size());
        this.sendMessages = new SendMessagesNotificationsImpl(this.torrentInfo,
                this.me, this.peer,
                this.peerCurrentStatus,
                this::closeConnection,
                dataOutputStream);
        this.receiveMessagesNotifications = new ReceiveMessagesNotificationsImpl(torrentInfo, this.me,
                this.peer, this.peerCurrentStatus, dataInputStream);

        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                this.receiveMessagesNotifications.getPeerMessageResponseFlux(),
                this.sendMessages.sentPeerMessagesFlux());
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
            //exception.printStackTrace();
        }
    }

    public SpeedStatistics getPeerSpeedStatistics() {
        return peerSpeedStatistics;
    }

    @Override
    public String toString() {
        return "Link{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }

    public ReceiveMessagesNotifications receivePeerMessages() {
        return receiveMessagesNotifications;
    }

    public SendMessagesNotifications sendMessages() {
        return sendMessages;
    }

    public PeerCurrentStatus getPeerCurrentStatus() {
        return peerCurrentStatus;
    }
}
