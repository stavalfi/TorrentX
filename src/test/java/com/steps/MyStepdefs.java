package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.file.system.ActiveTorrent;
import main.file.system.ActiveTorrents;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerExceptions;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.junit.Assert;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .flatMap(peersCommunicator -> peersCommunicator.receivePeerMessages().getHaveMessageResponseFlux()
                        .take(1)
                        .flatMap(haveMessage -> peersCommunicator.sendRequestMessage(haveMessage.getPieceIndex(), 0, requestBlockSize)))
                .flatMap(peersCommunicator ->
                        peersCommunicator.receivePeerMessages()
                                .getPieceMessageResponseFlux()
                                .doOnNext(pieceMessage -> peersCommunicator.closeConnection()))
                .take(1)
                .single();

        StepVerifier.create(receiveSinglePieceMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application create active-torrent for: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationCreateActiveTorrentFor(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mono<ActiveTorrent> activeTorrentMono =
                ActiveTorrents.getInstance()
                        .createActiveTorrentMono(torrentInfo, downloadLocation);

        StepVerifier.create(activeTorrentMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mono<Optional<ActiveTorrent>> activeTorrentMono =
                ActiveTorrents.getInstance()
                        .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash());

        StepVerifier.create(activeTorrentMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^files of torrent: \"([^\"]*)\" exist: \"([^\"]*)\" in \"([^\"]*)\"$")
    public void torrentExistIn(String torrentFileName, boolean torrentFilesExist, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Stream<File> fileStream = torrentInfo.getFileList()
                .stream()
                .map(TorrentFile::getFileDirs)
                .map(List::stream)
                .map((Stream<String> incompleteFilePath) -> incompleteFilePath.collect(Collectors.joining("/")))
                .map((String incompleteFilePath) -> downloadLocation.concat(incompleteFilePath))
                .map((String completeFilePath) -> new File(completeFilePath));

        if (torrentFilesExist)
            fileStream.peek(file -> Assert.assertTrue("file does not exist: " + file.getPath(), file.exists()))
                    .peek(file -> Assert.assertTrue("file is a directory: " + file.getPath(), !file.isDirectory()))
                    .peek(file -> Assert.assertTrue("we can't read from the file: " + file.getPath(), file.canRead()))
                    .forEach(file -> Assert.assertTrue("we can't write to the file: " + file.getPath(), file.canWrite()));
        else
            fileStream.forEach(file -> Assert.assertTrue("file exist: " + file.getPath(), file.exists()));
    }

    @Then("^application delete active-torrent: \"([^\"]*)\" and file: \"([^\"]*)\"$")
    public void applicationDeleteActiveTorrentAndFile(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mono<Optional<ActiveTorrent>> deleteTorrentTaskMono =
                ActiveTorrents.getInstance()
                        .deleteActiveTorrentAndFileMono(torrentInfo.getTorrentInfoHash())
                        .map(Optional::get)
                        // assert that the files of this torrent is not exist
                        .doOnNext(activeTorrent -> {
                            File file = new File(downloadLocation + torrentInfo.getName());
                            Assert.assertFalse("file eixst in local file system after we deleted it.", file.exists());
                        })
                        // check if this active torrent is not exist
                        .flatMap(activeTorrent -> ActiveTorrents.getInstance()
                                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash()))
                        .filter(Optional::isPresent)
                        .doOnNext(activeTorrent -> Assert.fail("active torret is stille exist after we deleted it."));

        StepVerifier.create(deleteTorrentTaskMono)
                // if the active torrent is deleted then filter won't pass anything.
                .verifyComplete();
    }

    @Then("^application save random blocks from different threads inside torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Function<Integer, byte[]> toRandomByteArray = length -> {
            byte[] bytes = new byte[length];
            byte content = 0;
            for (int i = 0; i < length; i++, content++)
                bytes[i] = content;
            return bytes;
        };

        Mono<ActiveTorrent> activeTorrentMono = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .map(Optional::get)
                .subscribeOn(Schedulers.elastic());

        Flux<byte[]> readFlux = Flux.fromIterable(blockList)
                .map(blockOfPiece -> new PieceMessage(null, null, blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                        toRandomByteArray.apply(blockOfPiece.getLength())))
                .flatMap(pieceMessage -> activeTorrentMono
                        .flatMap(activeTorrent -> activeTorrent.writeBlock(pieceMessage).subscribeOn(Schedulers.elastic()))
                        // assert that the content is written.
                        .map(activeTorrent -> {
                            RequestMessage requestMessage =
                                    new RequestMessage(null, null,
                                            pieceMessage.getIndex(),
                                            pieceMessage.getBegin(),
                                            pieceMessage.getBlock().length);
                            return Utils.readFromFile(torrentInfo, downloadLocation, requestMessage);
                        })
                        .doOnNext(readByteArray -> {
                            String message = "the content I wrote is not equal to the content I read to the file";
                            Assert.assertArrayEquals(message, readByteArray, pieceMessage.getBlock());
                        }));

        StepVerifier.create(readFlux)
                .expectNextCount(blockList.size())
                .verifyComplete();
    }

    @Then("^completed pieces are for torrent: \"([^\"]*)\":$")
    public void completedPiecesAreForTorrent(String torrentFileName, List<Integer> completedPiecesIndexList) throws Throwable {
        TorrentInfo torrentInfo = Utils.readTorrentFile(torrentFileName);

        Mono<ActiveTorrent> activeTorrentMono = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .map(Optional::get)
                .doOnNext(activeTorrent ->
                        completedPiecesIndexList.forEach(completedPiecesIndex -> {
                            String message = "the piece is not completed but it should be.";
                            Assert.assertTrue(message, activeTorrent.havePiece(completedPiecesIndex));
                        }));

        StepVerifier.create(activeTorrentMono)
                .expectNextCount(1)
                .verifyComplete();

    }

    private SpeedStatistics torrentDownloadSpeedStatistics;

    @Given("^size of incoming messages every \"([^\"]*)\" mill-seconds from a peer:$")
    public void sizeOfIncomingMessagesEveryMillSecondsFromAPeer(int delay, List<Integer> incomingMessageSizeList) throws Throwable {

        Flux<? extends PeerMessage> receivedMessageMessages = Flux.fromIterable(incomingMessageSizeList)
                .delayElements(Duration.ofMillis(delay))
                .map(incomingMessageSize ->
                        new PieceMessage(null, null, 0, 0, new byte[incomingMessageSize]));

        Flux<? extends PeerMessage> sentSentMessages = Flux.empty();

        this.torrentDownloadSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo, receivedMessageMessages, sentSentMessages);
    }

    @Then("^download statistics every 100 mill-seconds are from a peer:$")
    public void downloadStatisticsEveryMillSecondsAreFromAPeer(List<Double> downloadSpeedStatistics) throws Throwable {

        Flux<Tuple2<Double, Double>> speedComparisionFlux =
                Flux.zip(Flux.fromIterable(downloadSpeedStatistics),
                        this.torrentDownloadSpeedStatistics.getDownloadSpeedFlux())
                        .doOnNext(values -> {
                            String message = "download speed expected and actual are not equal";
                            Assert.assertEquals(message, values.getT1(), values.getT2());
                        });

        StepVerifier.create(speedComparisionFlux)
                .expectNextCount(downloadSpeedStatistics.size())
                .verifyComplete();
    }

    private SpeedStatistics torrentUploadSpeedStatistics;

    @Given("^size of outgoing messages every \"([^\"]*)\" mill-seconds from a peer:$")
    public void outgoingMessagesEveryMillSecondsFromAPeer(int delay, List<Integer> outgoingMessageSizeList) throws Throwable {
        Flux<? extends PeerMessage> receivedMessageMessages = Flux.fromIterable(outgoingMessageSizeList)
                .delayElements(Duration.ofMillis(delay))
                .map(outgoingMessageSize ->
                        new PieceMessage(null, null, 0, 0, new byte[outgoingMessageSize]));

        Flux<? extends PeerMessage> sentSentMessages = Flux.empty();
        this.torrentUploadSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo, receivedMessageMessages, sentSentMessages);
    }

    @Then("^upload statistics every 100 mill-seconds are from a peer:$")
    public void uploadStatisticsEveryMillSecondsAreFromAPeer(List<Double> uploadSpeedStatistics) throws Throwable {
        Flux<Tuple2<Double, Double>> speedComparisionFlux =
                Flux.zip(Flux.fromIterable(uploadSpeedStatistics),
                        this.torrentUploadSpeedStatistics.getDownloadSpeedFlux())
                        .doOnNext(values -> {
                            String message = "upload speed expected and actual are not equal";
                            Assert.assertEquals(message, values.getT1(), values.getT2());
                        });

        StepVerifier.create(speedComparisionFlux)
                .expectNextCount(uploadSpeedStatistics.size())
                .verifyComplete();
    }
}

