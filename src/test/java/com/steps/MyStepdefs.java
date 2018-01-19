package com.steps;

import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.*;
import main.peer.PeerMessage;
import main.tracker.*;
import main.tracker.response.TrackerResponse;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

    @Given("^new torrent file: \"([^\"]*)\".$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        this.torrentFilePath = torrentFileName;
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);
        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(torrentInfo.getTorrentInfoHash());
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(torrentInfo.getTrackerList());
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

    @Then("^application send Handshake request to a random peer.$")
    public void applicationSendCommunicationRequestToARandomPeer() throws Throwable {

    }

    @Then("^application receive Handshake response from the same peer.$")
    public void applicationReceiveHandshakeResponseFromTheSamePeer() throws Throwable {
        Mono<Peer> connectedPeerMono = PeersProvider.peers(this.torrentInfo.getTrackerList().stream())
                .flatMap(InitializePeersCommunication::initialize)
                .map(PeersCommunicator::getPeer)
                .take(1)
                .single();

        StepVerifier.create(connectedPeerMono)
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

    @Then("^communication with the peer failed: \"([^\"]*)\".$")
    public void communicationWithThePeerFailed(ErrorSignalType errorSignalType) throws Throwable {
        Flux<Peer> connectedPeerMono = PeersProvider.peers(this.torrentInfo.getTrackerList().stream())
                .flatMap(InitializePeersCommunication::initialize)
                .map(PeersCommunicator::getPeer);

        StepVerifier.create(connectedPeerMono)
                .expectError(errorSignalType.getErrorSignal())
                .verify();
    }

    @Then("^change the torrent-info-hash to a invalid torrent-info-hash.$")
    public void changeTheTorrentInfoHashToAInvalidTorrentInfoHash() throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(this.torrentFilePath);
        String fakeTorrentInfoHash = "0123456789012345678901234567890123456789";
        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(fakeTorrentInfoHash);
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(torrentInfo.getTrackerList());
    }

    @Given("^new torrent file: \"([^\"]*)\" containing the following fake peers:$")
    public void newTorrentFileContainingTheFollowingFakePeers(String torrentFilePath, List<Peer> peers) throws Throwable {
        this.torrentFilePath = torrentFilePath;
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFilePath);
        Mockito.when(this.torrentInfo.getTorrentInfoHash()).thenReturn(torrentInfo.getTorrentInfoHash());
        this.remoteFakePeers = peers.stream()
                .map(RemoteFakePeer::new)
                .peek(RemoteFakePeer::listen)
                .collect(Collectors.toList());
    }

    @Then("^application send in parallel and receive the following messages:$")
    public void applicationSendInParallelAndReceiveTheFollowingMessages(List<PeerFakeMessage> peerFakeMessages) throws Throwable {

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
                .forEach((PeerFakeMessage peer, List<PeerFakeMessage> messages) -> {
                    InitializePeersCommunication.initialize(new Peer(peer.getPeerIp(), peer.getPeerPort()))
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

                                boolean isSequenceShouldContainErrorSignal = messages.stream()
                                        .anyMatch(message -> message.getErrorSignalType() != null);

                                if (isSequenceShouldContainErrorSignal) {
                                    ErrorSignalType errorSignalType = messages.stream()
                                            .filter(message -> message.getErrorSignalType() != null)
                                            .findAny()
                                            .get() // isSequenceShouldContainErrorSignal == true
                                            .getErrorSignalType();
                                    messagesWeActuallyReceived.expectError(errorSignalType.getErrorSignal())
                                            .verify();
                                } else
                                    messagesWeActuallyReceived.expectComplete()
                                            .verify();

                                peersCommunicator.closeConnection();
                            });

                });

        this.remoteFakePeers.forEach(RemoteFakePeer::shutdown);
    }

    @Then("^application send and receive the following messages from a random tracker:$")
    public void applicationSendAndReceiveTheFollowingMessagesFromARandomTracker(List<TrackerFakeRequestResponseMessage> messages) throws Throwable {

        // if I get an errorSignal signal containing one of those errors,
        // then I will communicate with the next tracker in the tracker-list.
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        List<Tracker> trackers = this.torrentInfo.getTrackerList();
        Flux<? extends TrackerResponse> actualResponseFlux = Flux.fromIterable(trackers)
                .flatMap(tracker -> {
                    return Flux.fromIterable(messages)
                            // given a tracker, communicate with him and get the signal containing the response.
                            .flatMap(messageWeNeedToSend -> {
                                switch (messageWeNeedToSend.getTrackerRequestType()) {
                                    case Connect:
                                        return ConnectToTracker.connect(tracker.getTracker(), tracker.getPort());
                                    case Announce:
                                        return ConnectToTracker.connect(tracker.getTracker(), tracker.getPort())
                                                .flatMap(connectResponse ->
                                                        AnnounceToTracker.announce(connectResponse, torrentInfo.getTorrentInfoHash()));
                                    case Scrape:
                                        return ConnectToTracker.connect(tracker.getTracker(), tracker.getPort())
                                                .flatMap(connectResponse ->
                                                        ScrapeToTracker.scrape(connectResponse, Collections.singletonList(torrentInfo.getTorrentInfoHash())));
                                    default:
                                        throw new IllegalArgumentException(messageWeNeedToSend.getTrackerRequestType().toString());
                                }
                            })
                            // collect all the responses from this tracker until there are no more to a list.
                            .buffer()
                            // if something went wrong, return empty flux.
                            .onErrorResume(communicationErrorsToIgnore, error -> Mono.empty())
                            .flatMap(Flux::fromIterable);
                })
                // take is an operation which may send "cancel" signal.
                .take(messages.size());

        // check the responses

        Optional<ErrorSignalType> expectedErrorSignal = messages
                .stream()
                .map(TrackerFakeRequestResponseMessage::getErrorSignalType)
                .filter(error -> error.isPresent())
                .map(error -> error.get())
                .findAny();


        if (expectedErrorSignal.isPresent()) {
            StepVerifier.create(actualResponseFlux)
                    .expectError(expectedErrorSignal.get().getErrorSignal())
                    .verify();
            return;
        }

        StepVerifier.create(actualResponseFlux)
                .expectNextCount(messages.size())
                .expectComplete()
                .verify();
    }
}

