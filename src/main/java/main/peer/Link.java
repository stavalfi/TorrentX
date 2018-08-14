package main.peer;

import main.TorrentInfo;
import main.file.system.allocator.AllocatorStore;
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
    private AllocatorStore allocatorStore;
    private String identifier;

    // TODO: remove this copy consturctor - it is in use only in tests.
    public Link(Link link) {
        this(link.allocatorStore, link.torrentInfo, link.peer, link.peerSocket,
                link.dataInputStream, link.dataOutputStream, link.identifier);
    }

    public Link(AllocatorStore allocatorStore,
                TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                DataInputStream dataInputStream,
                DataOutputStream dataOutputStream,
                String identifier) {
        assert peerSocket != null;
        this.identifier = identifier;
        this.allocatorStore = allocatorStore;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.torrentInfo = torrentInfo;
        this.me = new Peer("localhost", peerSocket.getLocalPort());

        this.peerCurrentStatus = new PeerCurrentStatus(torrentInfo.getPieces().size());
        this.sendMessages = new SendMessagesNotificationsImpl(this.allocatorStore,
                this.torrentInfo,
                this.me, this.peer,
                this.peerCurrentStatus,
                this::closeConnection,
                dataOutputStream,
                identifier);
        this.receiveMessagesNotifications = new ReceiveMessagesNotificationsImpl(this.allocatorStore, torrentInfo, this.me,
                this.peer, this.peerCurrentStatus, dataInputStream, identifier);

        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                this.receiveMessagesNotifications.getPeerMessageResponseFlux(),
                this.sendMessages.sentPeerMessagesFlux());
    }

    public AllocatorStore getAllocatorStore() {
        return allocatorStore;
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
