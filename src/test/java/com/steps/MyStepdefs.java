package com.steps;

import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.peer.ConnectToPeer;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.tracker.BadResponseException;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.mockito.Mockito;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Predicate;

import static org.mockito.Mockito.mock;

public class MyStepdefs {

    private String torrentFilePath;
    private TorrentInfo torrentInfo = mock(TorrentInfo.class);

    @Given("^new torrent file: \"([^\"]*)\"$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        this.torrentFilePath = torrentFileName;
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mockito.when(this.torrentInfo.getTorrentInfoHash())
                .thenReturn(torrentInfo.getTorrentInfoHash());
        Mockito.when(this.torrentInfo.getTrackerList())
                .thenReturn(torrentInfo.getTrackerList());
    }

    @Given("^additional not-responding trackers to the tracker-list$")
    public void additionalNotRespondingTrackersToTheTrackerListFromFile() throws Throwable {
        List<Tracker> fakeTrackers = new ArrayList<>();

        // wrong url (but valid url) and a random port
        fakeTrackers.add(new Tracker("udp", "wrongUrl.com", 8090));

        this.torrentInfo.getTrackerList()
                .stream()
                .findFirst()
                .ifPresent(tracker -> fakeTrackers.add(new Tracker("udp", tracker.getTracker(), tracker.getPort() + 1))); // wrong port

        List<Tracker> trackers = new LinkedList<>();
        trackers.addAll(fakeTrackers);
        trackers.addAll(torrentInfo.getTrackerList());
        trackers.addAll(fakeTrackers);

        // update our mock object
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(trackers);
    }

    @Given("^additional invalid url of a tracker$")
    public void additionalInvalidUrlOfATrackerOf() throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(this.torrentFilePath);
        String torrentHashInfo = torrentInfo.getTorrentInfoHash();
        Mockito.when(this.torrentInfo.getTorrentInfoHash())
                .thenReturn(torrentHashInfo);
        Mockito.when(this.torrentInfo.getTrackerList())
                .thenReturn(Collections.singletonList(new Tracker("udp", "invalid.url.123", 123)));
    }

    @Then("^application send and receive Handshake from the same random peer$")
    public void applicationSendAndReceiveHandshakeFromTheSameRandomPeer() throws Throwable {
        TrackerProvider trackerProvider = new TrackerProvider(this.torrentInfo);

        // we won't listen for peers so we use illegal port number.
        int tcpPortToListenForPeers = -80;

        ConnectToPeer connectToPeer =
                new ConnectToPeer(this.torrentInfo, tcpPortToListenForPeers);

        PeersProvider peersProvider = new PeersProvider(this.torrentInfo, trackerProvider, connectToPeer);

        Mono<PeersCommunicator> peersCommunicatorFlux =
                trackerProvider.connectToTrackers()
                        .refCount(1)
                        .flatMap(trackerConnection -> peersProvider.connectToPeers(trackerConnection))
                        .take(1)
                        .single();

        StepVerifier.create(peersCommunicatorFlux)
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

    @Then("^application send and receive the following messages from a random tracker:$")
    public void applicationSendAndReceiveTheFollowingMessagesFromARandomTracker(List<TrackerFakeRequestResponseMessage> messages) throws Throwable {
        boolean isMessagesFormatGood = messages.stream()
                .noneMatch(fakeMessage -> fakeMessage.getTrackerRequestType() == TrackerRequestType.Connect);

        if (isMessagesFormatGood)
            throw new IllegalArgumentException("messages list must contain `connect` request" +
                    " (we are not using it in the tests but " +
                    "it should be there before any other request).");

        // if I get an errorSignal signal containing one of those errors,
        // then I will communicate with the next tracker in the tracker-list.
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        TrackerProvider trackerProvider = new TrackerProvider(this.torrentInfo);

        // we won't listen for peers so we use illegal port number.
        int tcpPortToListenForPeers = -80;

        ConnectToPeer connectToPeer =
                new ConnectToPeer(this.torrentInfo, tcpPortToListenForPeers);

        // we must to do for now and if not we get NullPointerException.
        connectToPeer.listen().subscribe();

        Flux<TrackerResponse> actualTrackerResponseFlux =
                trackerProvider.connectToTrackers()
                        .refCount(1)
                        .log()
                        .flatMap(trackerConnection ->
                                Flux.fromIterable(messages)
                                        .filter(fakeMessage -> fakeMessage.getTrackerRequestType() != TrackerRequestType.Connect)
                                        // given a tracker, communicate with him and get the signal containing the response.
                                        .flatMap(messageWeNeedToSend -> {
                                            switch (messageWeNeedToSend.getTrackerRequestType()) {
                                                case Announce:
                                                    return trackerConnection.announce(this.torrentInfo.getTorrentInfoHash(),
                                                            connectToPeer.getTcpPort());
                                                case Scrape:
                                                    return trackerConnection.scrape(Collections.singletonList(this.torrentInfo.getTorrentInfoHash()));
                                                default:
                                                    throw new IllegalArgumentException(messageWeNeedToSend.getTrackerRequestType().toString());
                                            }
                                        }).onErrorResume(communicationErrorsToIgnore, error -> Mono.empty()))
                        // unimportant note: take is an operation which send "cancel"
                        // signal if the flux contain more elements then we want.
                        .take(messages.size() - 1);

        // check the responses
        // check if there is an **expected** error signal:
        Optional<TrackerFakeRequestResponseMessage> expectedErrorSignal = messages
                .stream()
                .filter(message -> message.getErrorSignalType() != null)
                .findAny();

        if (expectedErrorSignal.isPresent())
            StepVerifier.create(actualTrackerResponseFlux)
                    .expectError(expectedErrorSignal.get().getErrorSignalType().getErrorSignal())
                    .verify();
        else
            StepVerifier.create(actualTrackerResponseFlux)
                    .expectNextCount(messages.size() - 1)
                    .expectComplete()
                    .verify();
    }

    @Then("^application send to \\[peer ip: \"([^\"]*)\", peer port: \"([^\"]*)\"\\] and receive the following messages:$")
    public void applicationSendToPeerIpPeerPortAndReceiveTheFollowingMessages(String peerIp, int peerPort,
                                                                              List<PeerFakeRequestResponse> peerFakeRequestResponses) throws Throwable {
        RemoteFakePeer remoteFakePeer = new RemoteFakePeer(new Peer(peerIp, peerPort));
        remoteFakePeer.listen();

        // we won't listen for peers so we use illegal port number.
        int tcpPortToListenForPeers = -80;

        ConnectToPeer connectToPeer =
                new ConnectToPeer(this.torrentInfo, tcpPortToListenForPeers);

        Mono<PeersCommunicator> peersCommunicatorMono = connectToPeer
                .connectToPeer(remoteFakePeer)
                .cache();

        // send all messages
        Flux<PeersCommunicator> peerRequestsFlux = Flux.fromIterable(peerFakeRequestResponses)
                .map(PeerFakeRequestResponse::getSendMessageType)
                .flatMap(peerRequestMessage -> peersCommunicatorMono.flux()
                        .flatMap(peersCommunicator -> Utils.sendFakeMessage(peerRequestMessage, peersCommunicator)));

        // check that all the messages sent successfully.
        StepVerifier.create(peerRequestsFlux)
                .expectNextCount(peerFakeRequestResponses.size())
                .verifyComplete();

        // receive all responses from peers.
        Flux<PeerMessage> peersResponses = peersCommunicatorMono.flux()
                .flatMap(PeersCommunicator::getPeerMessageResponseFlux);

        // check if we expect an error signal.
        Optional<ErrorSignalType> errorSignalType = peerFakeRequestResponses.stream()
                .filter(peerFakeRequestResponse -> peerFakeRequestResponse.getErrorSignalType() != null)
                .map(PeerFakeRequestResponse::getErrorSignalType)
                .findAny();

        // check if we expect a complete signal
        Optional<PeerFakeRequestResponse> completeSignal = peerFakeRequestResponses.stream()
                .filter(peerFakeRequestResponse -> peerFakeRequestResponse.getErrorSignalType() == null &&
                        peerFakeRequestResponse.getReceiveMessageType() == null)
                .findAny();

        if (completeSignal.isPresent())
            StepVerifier.create(peersResponses)
                    .expectNextCount(peerFakeRequestResponses.size() - 1)
                    .expectComplete()
                    .verify();
        else if (errorSignalType.isPresent())
            StepVerifier.create(peersResponses)
                    .expectNextCount(peerFakeRequestResponses.size() - 1)
                    .expectError(errorSignalType.get().getErrorSignal())
                    .verify();
        else
            StepVerifier.create(peersResponses.take(peerFakeRequestResponses.size()))
                    .expectNextCount(peerFakeRequestResponses.size())
                    .verifyComplete();

        peersCommunicatorMono.subscribe(PeersCommunicator::closeConnection);
        remoteFakePeer.shutdown();
    }

    @Then("^application interested in all peers$")
    public void applicationInterestedInAllPeers() throws Throwable {

    }

    @Then("^application request for a random block of a random piece from all peers$")
    public void applicationRequestForARandomBlockOfARandomPieceFromAllPeers() throws Throwable {

    }

    @Then("^application receive at list one random block of a random piece$")
    public void applicationReceiveAtListOneRandomBlockOfARandomPiece() throws Throwable {
        TrackerProvider trackerProvider = new TrackerProvider(this.torrentInfo);

        // we won't listen for peers so we use illegal port number.
        int tcpPortToListenForPeers = -80;

        ConnectToPeer connectToPeer =
                new ConnectToPeer(this.torrentInfo, tcpPortToListenForPeers);

        PeersProvider peersProvider = new PeersProvider(this.torrentInfo, trackerProvider, connectToPeer);

        ConnectableFlux<TrackerConnection> trackerConnectionConnectableFlux = trackerProvider.connectToTrackers();
        Flux<PeersCommunicator> peersCommunicatorFlux = peersProvider.connectToPeers(trackerConnectionConnectableFlux.autoConnect());

        int requestBlockSize = 16000;

        Mono<PieceMessage> receiveSingleBlockMono = peersCommunicatorFlux
                .flatMap(peersCommunicator -> peersCommunicator.sendInterestedMessage())
                .flatMap(peersCommunicator ->
                        peersCommunicator.getHaveMessageResponseFlux()
                                .flatMap(haveMessage ->
                                        peersCommunicator.sendRequestMessage(haveMessage.getPieceIndex(), 0, requestBlockSize)))
                .flatMap(PeersCommunicator::getPieceMessageResponseFlux)
                .take(1)
                .single();

        StepVerifier.create(receiveSingleBlockMono)
                .expectNextCount(1)
                .verifyComplete();
    }
}

