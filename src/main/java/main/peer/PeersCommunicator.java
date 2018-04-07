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
    private PeerCurrentStatus peerCurrentStatus;
    private SendPeerMessages sendMessages;
    private ReceivePeerMessages receivePeerMessages;
    private SpeedStatistics peerSpeedStatistics;

    public PeersCommunicator(TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                             DataInputStream dataInputStream,
                             DataOutputStream peerDataOutputStream) {
        assert peerSocket != null;
        this.peer = peer;
        this.peerSocket = peerSocket;
        this.torrentInfo = torrentInfo;
        this.me = new Peer("localhost", peerSocket.getLocalPort());

        this.peerCurrentStatus = new PeerCurrentStatus(torrentInfo.getPieces().size());
        this.sendMessages = new SendPeerMessagesImpl(this.me, this.peer,
                this.peerCurrentStatus,
                this::closeConnection,
                peerDataOutputStream);
        this.receivePeerMessages = new ReceivePeerMessagesImpl(this.me, this.peer,
                this.peerCurrentStatus,
                dataInputStream);

        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
                this.receivePeerMessages.getPeerMessageResponseFlux(),
                this.sendMessages.sentPeerMessagesFlux());
    }

    public ReceivePeerMessages receivePeerMessages() {
        return receivePeerMessages;
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
