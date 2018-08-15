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
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private TorrentInfo torrentInfo;
    private PeerCurrentStatus peerCurrentStatus;
    private SendMessagesNotifications sendMessages;
    private ReceivePeerMessages receiveMessagesNotifications;
    private SpeedStatistics peerSpeedStatistics;
    private AllocatorStore allocatorStore;
    private String identifier;
    private EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$;
    private FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages;

    // TODO: remove this copy constructor - it is in use only in tests.
    public Link(Link link) {
        this(link.allocatorStore, link.torrentInfo, link.peer, link.peerSocket,
                link.dataInputStream, link.dataOutputStream, link.identifier, link.incomingPeerMessages$, link.emitIncomingPeerMessages);
    }

    public Link(AllocatorStore allocatorStore,
                TorrentInfo torrentInfo, Peer peer, Socket peerSocket,
                DataInputStream dataInputStream,
                DataOutputStream dataOutputStream,
                String identifier,
                EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$,
                FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this.incomingPeerMessages$ = incomingPeerMessages$;
        this.emitIncomingPeerMessages = emitIncomingPeerMessages;
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

        this.receiveMessagesNotifications = new ReceivePeerMessages(this.allocatorStore, torrentInfo, this.me,
                this.peer, dataInputStream, identifier, this, emitIncomingPeerMessages);

        // TODO: move this to somewhere else.
//        this.peerSpeedStatistics = new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
//                this.receiveMessagesNotifications.getPeerMessageResponseFlux(),
//                this.sendMessages.sentPeerMessagesFlux());
    }

    public EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> getIncomingPeerMessages$() {
        return incomingPeerMessages$;
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
}
