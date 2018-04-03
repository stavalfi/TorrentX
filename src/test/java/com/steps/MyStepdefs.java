package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentPieceStatus;
import main.file.system.ActiveTorrent;
import main.file.system.ActiveTorrents;
import main.peer.Peer;
import main.peer.PeersCommunicator;
import main.peer.PeersListener;
import main.peer.PeersProvider;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.tracker.Tracker;
import main.tracker.TrackerExceptions;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.junit.Assert;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class MyStepdefs {

    static {
        // active debug mode in reactor
        Hooks.onOperatorDebug();
        // delete download folder from last test
        Utils.deleteDownloadFolder();
    }

    private TorrentInfo torrentInfo = mock(TorrentInfo.class);
    private static final String DEFAULT_DOWNLOAD_LOCATION = "torrents-test/";

    @Given("^new torrent file: \"([^\"]*)\"$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

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
                        .as(trackerConnectionFlux ->
                                peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionFlux)
                                        .autoConnect())
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

        int requestBlockSize = 16000;

        Mono<PieceMessage> receiveSinglePieceMono =
                trackerProvider.connectToTrackersFlux()
                        .autoConnect()
                        .as(trackerConnectionFlux -> peersProvider
                                .getPeersCommunicatorFromTrackerFlux(trackerConnectionFlux))
                        .autoConnect()
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
        Utils.createActiveTorrent(torrentFileName, downloadLocation);
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Mono<Optional<ActiveTorrent>> activeTorrentMono =
                ActiveTorrents.getInstance()
                        .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash());

        StepVerifier.create(activeTorrentMono)
                .consumeNextWith(activeTorrent -> {
                    String message = "activeTorrent object needs to be present:" + isActiveTorrentExist + ", but the opposite is heppening.";
                    Assert.assertEquals(message, isActiveTorrentExist, activeTorrent.isPresent());
                })
                .verifyComplete();
    }

    @Then("^files of torrent: \"([^\"]*)\" exist: \"([^\"]*)\" in \"([^\"]*)\"$")
    public void torrentExistIn(String torrentFileName, boolean torrentFilesExist, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        String fullFilePath = !torrentInfo.isSingleFileTorrent() ?
                System.getProperty("user.dir") + "/" + downloadLocation + torrentInfo.getName() + "/" :
                System.getProperty("user.dir") + "/" + downloadLocation;
        List<String> filePathList = torrentInfo.getFileList()
                .stream()
                .map(TorrentFile::getFileDirs)
                .map(List::stream)
                .map((Stream<String> incompleteFilePath) ->
                        incompleteFilePath.collect(Collectors.joining("/", fullFilePath, "")))
                .collect(Collectors.toList());

        if (torrentFilesExist) {
            String mainFilePath = System.getProperty("user.dir") + "/" + downloadLocation + torrentInfo.getName() + "/";
            File mainFile = new File(mainFilePath);
            if (torrentInfo.isSingleFileTorrent())
                Assert.assertTrue("file is directory but it doesn't need to be: " + mainFile.getPath(),
                        !mainFile.isDirectory());
            else
                Assert.assertTrue("file is not a directory but it needs to be: " + mainFile.getPath(),
                        mainFile.isDirectory());
            Flux<File> zip = Flux.zip(Flux.fromIterable(torrentInfo.getFileList()), Flux.fromIterable(filePathList),
                    (torrentFile, path) -> {
                        File file = new File(path);
                        Assert.assertEquals("file not in the right length: " + file.getPath(),
                                (long) torrentFile.getFileLength(), file.length());
                        return file;
                    })
                    .doOnNext(file -> Assert.assertTrue("file does not exist: " + file.getPath(), file.exists()))
                    .doOnNext(file -> Assert.assertTrue("we can't read from the file: " + file.getPath(), file.canRead()))
                    .doOnNext(file -> Assert.assertTrue("we can't write to the file: " + file.getPath(), file.canWrite()));
            StepVerifier.create(zip)
                    .expectNextCount(filePathList.size())
                    .verifyComplete();
        } else
            filePathList.stream()
                    .map((String completeFilePath) -> new File(completeFilePath))
                    .forEach(file -> Assert.assertTrue("file exist: " + file.getPath(), !file.exists()));
    }

    @Then("^application delete active-torrent: \"([^\"]*)\": \"([^\"]*)\" and file: \"([^\"]*)\"$")
    public void applicationDeleteActiveTorrentAndFile(String torrentFileName, boolean deleteActiveTorrent,
                                                      boolean deleteTorrentFiles) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Mono<Optional<ActiveTorrent>> deletionTaskMono = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .as(activeTorrentMono -> {
                    if (deleteTorrentFiles)
                        return activeTorrentMono.flatMap(activeTorrent ->
                                ActiveTorrents.getInstance()
                                        .deleteFileOnlyMono(torrentInfo.getTorrentInfoHash()));
                    return activeTorrentMono;
                })
                .as(activeTorrentMono -> {
                    if (deleteActiveTorrent)
                        return activeTorrentMono.flatMap(activeTorrent ->
                                ActiveTorrents.getInstance()
                                        .deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash()));
                    return activeTorrentMono;
                });

        StepVerifier.create(deletionTaskMono)
                .consumeNextWith(activeTorrent -> {
                    String message = "activeTorrent object wasn't exist.";
                    Assert.assertTrue(message, activeTorrent.isPresent());
                })
                .verifyComplete();
    }

    @Then("^application save random blocks from different threads inside torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        ActiveTorrent activeTorrent = Utils.createActiveTorrent(torrentFileName, downloadLocation);
        String fullDownloadPath = System.getProperty("user.dir") + "/" + downloadLocation;


        Function<Integer, byte[]> toRandomByteArray = (Integer length) -> {
            byte[] bytes = new byte[length];
            byte content = 0;
            for (int i = 0; i < length; i++, content++)
                bytes[i] = content;
            return bytes;
        };

        Flux<PieceMessage> pieceMessageFlux = Flux.fromIterable(blockList)
                .map(blockOfPiece -> {
                    if (blockOfPiece.getLength() != null)
                        return new PieceMessage(null, null, blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                                toRandomByteArray.apply(blockOfPiece.getLength()));
                    return new PieceMessage(null, null, blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                            toRandomByteArray.apply(activeTorrent.getPieceLength() - blockOfPiece.getFrom()));
                });

        Flux<RequestMessage> assertWrittenPiecesFlux = Flux.zip(activeTorrent.downloadAsync(pieceMessageFlux), pieceMessageFlux,
                (torrentPieceChanged, pieceMessage) -> {
                    RequestMessage requestMessage =
                            new RequestMessage(null, null,
                                    pieceMessage.getIndex(),
                                    pieceMessage.getBegin(),
                                    pieceMessage.getBlock().length);
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestMessage);
                    String message = "the content I wrote is not equal to the content I read to the file";
                    Assert.assertArrayEquals(message, actualWrittenBytes, pieceMessage.getBlock());

                    return requestMessage;
                });

        StepVerifier.create(assertWrittenPiecesFlux)
                .expectNextCount(blockList.size())
                .verifyComplete();
    }

    @Then("^completed pieces are for torrent: \"([^\"]*)\":$")
    public void completedPiecesAreForTorrent(String torrentFileName, List<Integer> completedPiecesIndexList) throws Throwable {
        ActiveTorrent activeTorrent = Utils.createActiveTorrent(torrentFileName, DEFAULT_DOWNLOAD_LOCATION);
        String errorMesssage1 = "the piece is not completed but it should be.";
        String errorMesssage2 = "The read operation failed to read exactly what we wrote";

        completedPiecesIndexList.forEach(completedPiecesIndex -> {
            Assert.assertTrue(errorMesssage1, activeTorrent.havePiece(completedPiecesIndex));
        });

        // check again in other way: (by ActiveTorrent::getAllPiecesStatus)
        BitFieldMessage allPiecesStatus = activeTorrent.getAllPiecesStatus(null, null);
        completedPiecesIndexList.forEach(completedPiecesIndex -> {
            Assert.assertTrue(errorMesssage1, allPiecesStatus.getPieces().get(completedPiecesIndex));
        });

        // check again in other way: (by ActiveTorrent::readBlock)
        String fullDownloadPath = System.getProperty("user.dir") + "/" + DEFAULT_DOWNLOAD_LOCATION;

        Flux<PieceMessage> completedPiecesMessageFlux = Flux.fromIterable(completedPiecesIndexList)
                .map(pieceIndex -> new RequestMessage(null, null,
                        pieceIndex,
                        0,
                        activeTorrent.getPieceLength()))
                // if the above piece is not completed, ActiveTorrent::readBlock will throw exception.
                // but it must complete because the piece is in completedPiecesIndexList list.
                .flatMap(requestMessage -> activeTorrent.readBlock(requestMessage))
                .doOnNext(pieceMessage -> {
                    RequestMessage requestMessage =
                            new RequestMessage(null, null,
                                    pieceMessage.getIndex(),
                                    pieceMessage.getBegin(),
                                    pieceMessage.getBlock().length);
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestMessage);
                    Assert.assertArrayEquals(errorMesssage2, actualWrittenBytes, pieceMessage.getBlock());
                });

        StepVerifier.create(completedPiecesMessageFlux)
                .expectNextCount(completedPiecesIndexList.size())
                .verifyComplete();
    }

    @Then("^application save all the last piece of torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationSaveAllTheLastPieceOfTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        ActiveTorrent activeTorrent = Utils.createActiveTorrent(torrentFileName, downloadLocation);

        String fullDownloadPath = System.getProperty("user.dir") + "/" + downloadLocation;

        int lastPieceLength = (int) Math.min(activeTorrent.getPieceLength(),
                activeTorrent.getTotalSize() - (activeTorrent.getPieces().size() - 1) * activeTorrent.getPieceLength());

        // generate random complete piece.
        byte[] lastPiece = new byte[lastPieceLength];
        byte content = 0;
        for (int i = 0; i < lastPieceLength; i++, content++)
            lastPiece[i] = content;

        int lastPieceIndex = activeTorrent.getPieces().size() - 1;
        PieceMessage lastPieceMessage = new PieceMessage(null, null, lastPieceIndex, 0, lastPiece);
        RequestMessage requestLastPieceMessage = new RequestMessage(null, null,
                lastPieceMessage.getIndex(),
                lastPieceMessage.getBegin(),
                lastPieceMessage.getBlock().length);

        Mono<PieceMessage> readLastPieceTaskMono = activeTorrent.downloadAsync(Flux.just(lastPieceMessage))
                .doOnNext(torrentPieceChanged -> {
                    String message = "the last piece must be completed but it's not.";
                    Assert.assertEquals(message, TorrentPieceStatus.COMPLETED, torrentPieceChanged.getTorrentPieceStatus());
                })
                // assert that we wrote to the file what we should have.
                .doOnNext(torrentPieceChanged -> {
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestLastPieceMessage);
                    String errorMesssage = "The read operation failed to read exactly what we wrote";
                    Assert.assertArrayEquals(errorMesssage, actualWrittenBytes, lastPieceMessage.getBlock());
                })
                // assert that we can read the last piece successfully.
                .flatMap(torrentPieceChanged -> activeTorrent.readBlock(requestLastPieceMessage))
                .doOnNext(pieceMessage -> {
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestLastPieceMessage);
                    String errorMesssage = "The read operation failed to read exactly what we wrote";
                    Assert.assertArrayEquals(errorMesssage, actualWrittenBytes, pieceMessage.getBlock());
                })
                // there must be only one signal here: the last piece. so signle() must work.
                .single();

        StepVerifier.create(readLastPieceTaskMono)
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

    @Then("^application connect to all peers and assert that we connected to them - for torrent: \"([^\"]*)\"$")
    public void applicationConnectToAllPeersAndAssertThatWeConnectedToThemForTorrent(String torrentFileName) throws Throwable {
        ActiveTorrent activeTorrent = Utils.createActiveTorrent(torrentFileName, DEFAULT_DOWNLOAD_LOCATION);

        TorrentDownloader torrentDownloader = TorrentDownloader.defaultTorrentDownloader(activeTorrent);

        // consume new peers and new responses from 1.5 seconds.
        // filter distinct peers from the responses, and assert
        // that both the list of peers are equal.

        Flux<Peer> connectedPeersFlux = torrentDownloader.getPeersCommunicatorFlux()
                .map(PeersCommunicator::getPeer)
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .doOnNext(x -> System.out.println("connected until now: " + x))
                .take(3)
                .flatMap(Flux::fromIterable)
                .sort();

        Flux<Peer> peersFromResponsesMono = torrentDownloader.getBittorrentAlgorithm()
                .receiveTorrentMessagesMessagesFlux()
                .getPeerMessageResponseFlux()
                .map(PeerMessage::getFrom)
                .distinct()
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .doOnNext(x -> System.out.println("responses from until now: " + x))
                .take(2)
                .flatMap(Flux::fromIterable)
                .sort()
                // I'm going to get this peers again AFTER:
                // torrentDownloader.getDownloadControl().start();
                .replay()
                .autoConnect();

        // for recording all the peers without blocking the main thread.
        peersFromResponsesMono.subscribe();

        torrentDownloader.getDownloadControl().start();

        List<Peer> connectedPeers = connectedPeersFlux.collectList().block();
        List<Peer> peersFromResponses = peersFromResponsesMono.collectList().block();

        peersFromResponses.stream()
                .filter(peer -> connectedPeers.contains(peer))
                .findFirst()
                .ifPresent(peer -> Assert.fail("We received from the following peer" +
                        " messages but he doesn't exist in the conencted peers flux: " + peer));
    }
}

