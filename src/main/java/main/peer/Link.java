package main.peer;

import main.TorrentInfo;
import main.file.system.allocator.AllocatorStore;
import main.peer.peerMessages.PeerMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.Objects;

public class Link {
    private Peer me;
    private Peer peer;
    private Socket peerSocket;
    private TorrentInfo torrentInfo;
    private PeerCurrentStatus peerCurrentStatus;
    private SendMessagesNotifications sendMessages;
    private ReceivePeerMessages receiveMessagesNotifications;
    private SpeedStatistics peerSpeedStatistics;
    private AllocatorStore allocatorStore;

    public Link(AllocatorStore allocatorStore,
                TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                DataInputStream dataInputStream,
                DataOutputStream dataOutputStream,
                String identifier,
                FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        assert peerSocket != null;
        this.allocatorStore = allocatorStore;
        this.peer = peer;
        this.peerSocket = peerSocket;
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

        this.receiveMessagesNotifications = new ReceivePeerMessages(this.allocatorStore, torrentInfo, this.me,
                this.peer, dataInputStream, identifier, this, emitIncomingPeerMessages);

        // TODO: move this to somewhere else.
//        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
//                this.receiveMessagesNotifications.getPeerMessageResponseFlux(),
//                this.sendMessages.sentPeerMessages$());
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

    private void closeConnection() {
        try {
            this.peerSocket.close();
        } catch (IOException exception) {
            // TODO: do something better... it's a fatal problem with my design!!!
            //exception.printStackTrace();
        }
    }

    public void dispose() {
        closeConnection();
        this.receivePeerMessages().dispose();
    }

    public SpeedStatistics getPeerSpeedStatistics() {
        return peerSpeedStatistics;
    }

    public ReceivePeerMessages receivePeerMessages() {
        return receiveMessagesNotifications;
    }

    public SendMessagesNotifications sendMessages() {
        return sendMessages;
    }

    public PeerCurrentStatus getPeerCurrentStatus() {
        return peerCurrentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(me, link.me) &&
                Objects.equals(peer, link.peer) &&
                Objects.equals(torrentInfo, link.torrentInfo);
    }

    @Override
    public int hashCode() {

        return Objects.hash(me, peer, torrentInfo);
    }

    @Override
    public String toString() {
        return "Link{" +
                "me=" + me +
                ", peer=" + peer +
                '}';
    }
}
