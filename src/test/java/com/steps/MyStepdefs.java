package com.steps;

import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.peer.InitializePeersCommunication;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.tracker.BadResponseException;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

public class MyStepdefs {

    private String torrentFilePath;

    private TorrentInfo torrentInfo = mock(TorrentInfo.class);

    private List<RemoteFakePeer> remoteFakePeers;
    private InitializePeersCommunication initializePeersCommunication;

    @Given("^new torrent file: \"([^\"]*)\".$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        this.torrentFilePath = torrentFileName;
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        this.initializePeersCommunication = new InitializePeersCommunication(torrentInfo.getTorrentInfoHash());

        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(torrentInfo.getTorrentInfoHash());
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(torrentInfo.getTrackerList());
    }

    @Then("^change the torrent-info-hash to a invalid torrent-info-hash.$")
    public void changeTheTorrentInfoHashToAInvalidTorrentInfoHash() throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(this.torrentFilePath);
        String fakeTorrentInfoHash = "0123456789012345678901234567890123456789";
        assert fakeTorrentInfoHash.length() == 40;
        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(fakeTorrentInfoHash);
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(torrentInfo.getTrackerList());

        // I can safely restart this object because no one connected
        // to me and I didn't connect to no one yet.
        this.initializePeersCommunication.stopListenForNewPeers();
        this.initializePeersCommunication = new InitializePeersCommunication(this.torrentInfo.getTorrentInfoHash());
    }

    @Given("^additional not-responding trackers to the tracker-list.$")
    public void additionalNotRespondingTrackersToTheTrackerListFromFile() throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(this.torrentFilePath);
        String torrentHashInfo = torrentInfo.getTorrentInfoHash();
        torrentInfo.getTrackerList().stream().findFirst().ifPresent(tracker -> {
            List<Tracker> fakeTrackers = Arrays.asList(
                    new Tracker("wrongUrl.com", 8090), // wrong url (but valid url) and a random port
                    new Tracker(tracker.getTracker(), tracker.getPort() + 1) // wrong port
            );
            List<Tracker> trackers = new LinkedList<>();
            trackers.addAll(fakeTrackers);
            trackers.addAll(torrentInfo.getTrackerList());
            trackers.addAll(fakeTrackers);

            // define our mock object
            Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(torrentHashInfo);
            Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(trackers);
        });
    }

    @Given("^additional invalid url of a tracker.$")
    public void additionalInvalidUrlOfATrackerOf() throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(this.torrentFilePath);
        String torrentHashInfo = torrentInfo.getTorrentInfoHash();
        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(torrentHashInfo);
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(Collections.singletonList(new Tracker("invalid.url.123", 123)));
    }

    @Given("^new torrent file: \"([^\"]*)\" containing the following fake peers:$")
    public void newTorrentFileContainingTheFollowingFakePeers(String torrentFilePath, List<Peer> peers) throws Throwable {
        this.remoteFakePeers = peers.stream()
                .map(RemoteFakePeer::new)
                .peek(RemoteFakePeer::listen)
                .collect(Collectors.toList());
    }

    @Then("^application send Handshake request to a random peer.$")
    public void applicationSendCommunicationRequestToARandomPeer() throws Throwable {

    }

    @Then("^application receive Handshake response from the same random peer.$")
    public void applicationReceiveHandshakeResponseFromTheSamePeer() throws Throwable {
        Mono<PeersCommunicator> connectedPeerMono =
                TrackerProvider.connectToTrackers(this.torrentInfo.getTrackerList())
                        .flatMap((TrackerConnection trackerConnection) ->
                                PeersProvider.connectToPeers(trackerConnection, this.initializePeersCommunication))
                        .take(1)
                        .single();

        StepVerifier.create(connectedPeerMono)
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

    @Then("^communication with the random peer failed: \"([^\"]*)\".$")
    public void communicationWithThePeerFailed(ErrorSignalType errorSignalType) throws Throwable {
        Mono<PeersCommunicator> connectedPeerMono =
                TrackerProvider.connectToTrackers(this.torrentInfo.getTrackerList())
                        .flatMap((TrackerConnection trackerConnection) ->
                                PeersProvider.connectToPeers(trackerConnection, this.initializePeersCommunication))
                        .take(1)
                        .single();

        StepVerifier.create(connectedPeerMono)
                .expectError(errorSignalType.getErrorSignal())
                .verify();
    }

    @Then("^application send and receive the following messages from a random tracker:$")
    public void applicationSendAndReceiveTheFollowingMessagesFromARandomTracker(List<TrackerFakeRequestResponseMessage> messages) throws Throwable {

        if (messages.stream()
                .noneMatch(fakeMessage -> fakeMessage.getTrackerRequestType() == TrackerRequestType.Connect))
            throw new IllegalArgumentException("messages list must contain `connect` request.");

        // if I get an errorSignal signal containing one of those errors,
        // then I will communicate with the next tracker in the tracker-list.
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        Flux<TrackerResponse> actualTrackerResponseFlux =
                TrackerProvider.connectToTrackers(this.torrentInfo.getTrackerList())
                        .flatMap(trackerConnection ->
                                Flux.fromIterable(messages)
                                        .filter(fakeMessage -> fakeMessage.getTrackerRequestType() != TrackerRequestType.Connect)
                                        // given a tracker, communicate with him and get the signal containing the response.
                                        .flatMap(messageWeNeedToSend -> {
                                            switch (messageWeNeedToSend.getTrackerRequestType()) {
                                                case Announce:
                                                    return trackerConnection.announce(this.torrentInfo.getTorrentInfoHash());
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

        if (expectedErrorSignal.isPresent()) {
            StepVerifier.create(actualTrackerResponseFlux)
                    .expectError(expectedErrorSignal.get().getErrorSignalType().getErrorSignal())
                    .verify();
            return;
        }

        StepVerifier.create(actualTrackerResponseFlux)
                .expectNextCount(messages.size() - 1)
                .expectComplete()
                .verify();
    }

    @Then("^application send in parallel and receive the following messages:$")
    public void applicationSendInParallelAndReceiveTheFollowingMessages(List<PeerFakeMessage> peerFakeMessages) throws
            Throwable {

        // What are we doing here:
        // groupBy all messages by peers. for each peer: connect to him,
        // send him all the messages from peerFakeMessages
        // list UNTIL I see a message which it's response is error signal.
        // After sending the messages (maybe not all), then wait
        // for a response. The response may be a message or an error signal.
        // after I verified that the messages/error signal came back,
        // shut down the socket with this peer.
        // then the scenario is over so we must shut down all
        // the remoteFakePeers servers also.

        peerFakeMessages.stream()
                .collect(Collectors.groupingBy(Function.identity()))
                .forEach((PeerFakeMessage peer, List<PeerFakeMessage> messages) ->
                        this.initializePeersCommunication.connectToPeer(new Peer(peer.getPeerIp(), peer.getPeerPort()))
                                .subscribe((PeersCommunicator peersCommunicator) -> {
                                    List<PeerMessage> messagesWeSend = messages.stream()
                                            .map(PeerFakeMessage::getSendMessageType)
                                            .map(PeerMessageType::getSignal)
                                            .map(Utils::createFakeMessage)
                                            .collect(Collectors.toList());

                                    List<PeerMessage> messagesWeShouldReceive = messages.stream()
                                            .filter(message -> message.getErrorSignalType() != null)
                                            .map(PeerFakeMessage::getReceiveMessageType)
                                            .map(PeerMessageType::getSignal)
                                            .map(Utils::createFakeMessage)
                                            .sorted()
                                            .collect(Collectors.toList());

                                    StepVerifier.Step<PeerMessage> messagesWeActuallyReceived =
                                            StepVerifier.create(Flux.fromIterable(messagesWeSend)
                                                    .flatMap(peersCommunicator::send))
                                                    .expectNextCount(messagesWeShouldReceive.size());

                                    boolean isSequenceContainErrorSignal = messages.stream()
                                            .anyMatch(message -> message.getErrorSignalType() != null);

                                    if (isSequenceContainErrorSignal) {
                                        messages.stream()
                                                .filter(message -> message.getErrorSignalType() != null)
                                                .map(PeerFakeMessage::getErrorSignalType)
                                                .findAny()
                                                .ifPresent((ErrorSignalType errorSignalType) ->
                                                        messagesWeActuallyReceived
                                                                .expectError(errorSignalType.getErrorSignal())
                                                                .verify());

                                    } else
                                        messagesWeActuallyReceived.expectComplete()
                                                .verify();

                                    try {
                                        peersCommunicator.closeConnection();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }));

        this.remoteFakePeers.forEach(RemoteFakePeer::shutdown);
    }

}

