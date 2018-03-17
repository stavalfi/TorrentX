package com.steps;

import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerExceptions;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

public class MyStepdefs {

    static {
        Hooks.onOperatorDebug();
    }

    private TorrentInfo torrentInfo = mock(TorrentInfo.class);

    @Given("^new torrent file: \"([^\"]*)\"$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mockito.when(this.torrentInfo.getTorrentFilePath())
                .thenReturn(torrentInfo.getTorrentFilePath());
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
                .ifPresent(tracker -> fakeTrackers.add(new Tracker("udp", tracker.getTrackerUrl(), tracker.getUdpPort() + 1))); // wrong port

        List<Tracker> trackers = new LinkedList<>();
        trackers.addAll(fakeTrackers);
        trackers.addAll(torrentInfo.getTrackerList());
        trackers.addAll(fakeTrackers);

        // update our mock object
        Mockito.when(this.torrentInfo.getTrackerList()).thenReturn(trackers);
    }

    @Given("^only one invalid url of a tracker$")
    public void additionalInvalidUrlOfATrackerOf() throws Throwable {
        Mockito.when(this.torrentInfo.getTrackerList())
                .thenReturn(Collections.singletonList(new Tracker("udp", "invalid.url.123", 123)));
    }

    @Then("^application send and receive Handshake from the same random peer$")
    public void applicationSendAndReceiveHandshakeFromTheSameRandomPeer() throws Throwable {
        TrackerProvider trackerProvider = new TrackerProvider(this.torrentInfo);
        PeersProvider peersProvider = new PeersProvider(this.torrentInfo);

        Mono<PeersCommunicator> peersCommunicatorFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect()
                        .transform(trackerConnectionFlux -> peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionFlux))
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
                    "it should be there before any other request for readability of the scenario).");

        TrackerProvider trackerProvider = new TrackerProvider(this.torrentInfo);

        Flux<TrackerResponse> actualTrackerResponseFlux = trackerProvider.connectToTrackersFlux()
                .autoConnect()
                .flatMap(trackerConnection ->
                        Flux.fromIterable(messages)
                                .filter(fakeMessage -> fakeMessage.getTrackerRequestType() != TrackerRequestType.Connect)
                                // given a tracker, communicateMono with him and get the signal containing the response.
                                .flatMap(messageWeNeedToSend -> {
                                    switch (messageWeNeedToSend.getTrackerRequestType()) {
                                        case Announce:
                                            return trackerConnection.announceMono(this.torrentInfo.getTorrentInfoHash(),
                                                    PeersListener.getInstance().getTcpPort());
                                        case Scrape:
                                            return trackerConnection.scrapeMono(Collections.singletonList(this.torrentInfo.getTorrentInfoHash()));
                                        default:
                                            throw new IllegalArgumentException(messageWeNeedToSend.getTrackerRequestType().toString());
                                    }
                                }).onErrorResume(TrackerExceptions.communicationErrors, error -> Mono.empty()))
                // we take one less because we won't get connectResponse here. We got it inside
                // trackerProvider.connectToTrackersFlux() for each tracker we tried to connect to.
                .take(messages.size() - 1);

        // check the responses
        // check if there is an **expected** error signal:
        Optional<TrackerFakeRequestResponseMessage> expectedErrorSignal = messages
                .stream()
                .filter(message -> message.getErrorSignalType() != null)
                .findAny();

        // we expect to get error signal before any next signal!!! the scenario must support it!
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

    @Then("^application send to \\[peer ip: \"([^\"]*)\", peer port: \"([^\"]*)\"] and receive the following messages:$")
    public void applicationSendToPeerIpPeerPortAndReceiveTheFollowingMessages(String peerIp, int peerPort,
                                                                              List<PeerFakeRequestResponse> peerFakeRequestResponses) throws Throwable {
        RemoteFakePeer remoteFakePeer = new RemoteFakePeer(new Peer(peerIp, peerPort));
        remoteFakePeer.listen();

        PeersProvider peersProvider = new PeersProvider(this.torrentInfo);

        List<PeerMessageType> peerMessageTypeList = peerFakeRequestResponses.stream()
                .map(PeerFakeRequestResponse::getSendMessageType)
                .collect(Collectors.toList());

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

        Mono<Object[]> peerResponsesMono = peersProvider.connectToPeerMono(remoteFakePeer)
                .flatMap(peersCommunicator -> {
                    List<Mono<PeersCommunicator>> sendPeerMessageMonoStream = peerMessageTypeList.stream()
                            .map(peerRequestMessage -> Utils.sendFakeMessage(peersCommunicator, peerRequestMessage))
                            .collect(Collectors.toList());
                    return Mono.zip(sendPeerMessageMonoStream, peersCommunicators -> peersCommunicator);
                })
                .flux()
                .flatMap(peersCommunicator -> {
                    List<Flux<? extends PeerMessage>> peerMessageFluxList = peerMessageTypeList.stream()
                            .map(peerMessageType -> Utils.getSpecificMessageResponseFluxByMessageType(peersCommunicator, peerMessageType))
                            .collect(Collectors.toList());
                    int listSize = peerMessageFluxList.size();
                    if (completeSignal.isPresent())
                        // we won't get any response for the last flux so we won't listen to it.
                        return Flux.zip(peerMessageFluxList.subList(0, listSize - 2), Function.identity())
                                .doOnNext(peerResponses -> peersCommunicator.closeConnection());
                    // there will be an error signal or only next signals. either way, we will listen to all fluxes.
                    return Flux.zip(peerMessageFluxList, Function.identity())
                            .doOnNext(peerResponses -> peersCommunicator.closeConnection());
                })
                .take(1)
                .single();

        StepVerifier.create(peerResponsesMono)
                .expectNextCount(1)
                .verifyComplete();

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
        PeersProvider peersProvider = new PeersProvider(this.torrentInfo);

        Flux<TrackerConnection> trackerConnectionConnectableFlux =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect(1);

        int requestBlockSize = 16000;

        Mono<PieceMessage> receiveSinglePieceMono = peersProvider
                .getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
                .flatMap(peersCommunicator -> peersCommunicator.sendInterestedMessage())
                .flatMap(peersCommunicator -> peersCommunicator.getHaveMessageResponseFlux()
                        .take(1)
                        .flatMap(haveMessage -> peersCommunicator.sendRequestMessage(haveMessage.getPieceIndex(), 0, requestBlockSize)))
                .flatMap(peersCommunicator -> peersCommunicator.getPieceMessageResponseFlux())
                .doOnNext(pieceMessage -> pieceMessage.getPeersCommunicator().closeConnection())
                .take(1)
                .single();

        StepVerifier.create(receiveSinglePieceMono)
                .expectNextCount(1)
                .verifyComplete();
    }
}

