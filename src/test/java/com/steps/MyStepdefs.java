package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.AppConfig;
import main.TorrentInfo;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.downloader.TorrentPieceStatus;
import main.file.system.ActiveTorrent;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.*;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusController;
import main.torrent.status.TorrentStatusControllerImpl;
import main.torrent.status.TorrentStatusType;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerExceptions;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.junit.Assert;
import org.mockito.Mockito;
import reactor.core.publisher.ConnectableFlux;
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

    @Given("^new torrent file: \"([^\"]*)\"$")
    public void newTorrentFile(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);

        Mockito.when(this.torrentInfo.getTorrentFilePath())
                .thenReturn(torrentInfo.getTorrentFilePath());
        Mockito.when(this.torrentInfo.getTorrentInfoHash())
                .thenReturn(torrentInfo.getTorrentInfoHash());
        Mockito.when(this.torrentInfo.getTrackerList())
                .thenReturn(torrentInfo.getTrackerList());
    }

    @Given("^additional not-responding trackers to the tracker-list$")
    public void additionalNotRespondingTrackersToTheTrackerListFromFile() {
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
    public void additionalInvalidUrlOfATrackerOf() {
        Mockito.when(this.torrentInfo.getTrackerList())
                .thenReturn(Collections.singletonList(new Tracker("udp", "invalid.url.123", 123)));
    }

    @Then("^application send and receive the following messages from a random tracker:$")
    public void applicationSendAndReceiveTheFollowingMessagesFromARandomTracker(List<TrackerFakeRequestResponseMessage> messages) {
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
                                                    AppConfig.getInstance().getMyListeningPort());
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
                                                                              List<PeerFakeRequestResponse> peerFakeRequestResponses) {
        RemoteFakePeerCopyCat remoteFakePeerCopyCat = new RemoteFakePeerCopyCat(new Peer(peerIp, peerPort));
        remoteFakePeerCopyCat.listen();

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

        Mono<Object[]> peerResponsesMono = peersProvider.connectToPeerMono(remoteFakePeerCopyCat)
                .flatMap(peersCommunicator -> {
                    List<Mono<SendPeerMessages>> sendPeerMessageMonoStream = peerMessageTypeList.stream()
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

        remoteFakePeerCopyCat.shutdown();
    }

    @Then("^application interested in all peers for torrent: \"([^\"]*)\"$")
    public void applicationInterestedInAllPeersForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
    }

    @Then("^application request for a random block of a random piece from all peers in torrent: \"([^\"]*)\"$")
    public void applicationRequestForARandomBlockOfARandomPieceFromAllPeersInTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
    }

    @Then("^application receive at list one random block of a random piece in torrent: \"([^\"]*)\"$")
    public void applicationReceiveAtListOneRandomBlockOfARandomPieceInTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        int requestBlockSize = 16000;

        Mono<PieceMessage> receiveSinglePieceMono = torrentDownloader.getPeersCommunicatorFlux()
                .flatMap(peersCommunicator -> peersCommunicator.sendMessages()
                        .sendInterestedMessage()
                        .map(sendPeerMessages -> peersCommunicator))
                .flatMap(peersCommunicator -> peersCommunicator.receivePeerMessages().getHaveMessageResponseFlux()
                        .take(1)
                        .flatMap(haveMessage -> peersCommunicator.sendMessages()
                                .sendRequestMessage(haveMessage.getPieceIndex(), 0, requestBlockSize)
                                .map(sendPeerMessages -> peersCommunicator)))
                .flatMap(peersCommunicator ->
                        peersCommunicator.receivePeerMessages()
                                .getPieceMessageResponseFlux()
                                .doOnNext(pieceMessage -> peersCommunicator.closeConnection()))
                .take(1)
                .single();

//        torrentDownloader.getTorrentStatusController().startUpload();

        StepVerifier.create(receiveSinglePieceMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application create active-torrent for: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationCreateActiveTorrentFor(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);

        // this will create an activeTorrent object.
        TorrentDownloader torrentDownloader = Utils.createDefaultTorrentDownloader(torrentInfo, System.getProperty("user.dir") + "/" + downloadLocation);
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Mono<Optional<ActiveTorrent>> activeTorrentMono =
                ActiveTorrents.getInstance()
                        .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash());

        StepVerifier.create(activeTorrentMono)
                .consumeNextWith(activeTorrent -> {
                    String message = "activeTorrent object needs to be present:" + isActiveTorrentExist +
                            ", but the opposite is happening.";
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
            Assert.assertTrue("main-folder/single-file does not exist: " + mainFile.getPath(), mainFile.exists());
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

        TorrentFileSystemManager torrentFileSystemManager = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getTorrentFileSystemManager();
        Mono<Boolean> deletionTaskMono;
        if (deleteTorrentFiles && deleteActiveTorrent)
            deletionTaskMono = torrentFileSystemManager.deleteFileOnlyMono(torrentInfo.getTorrentInfoHash())
                    .flatMap(areFilesRemoved ->
                            torrentFileSystemManager.deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash()));
        else if (deleteTorrentFiles)
            deletionTaskMono = torrentFileSystemManager.deleteFileOnlyMono(torrentInfo.getTorrentInfoHash());
        else // deleteActiveTorrent == true
            deletionTaskMono = torrentFileSystemManager.deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash());

        StepVerifier.create(deletionTaskMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application save random blocks for torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);

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
                    long blockLength = torrentInfo.getPieceLength();
                    if (blockOfPiece.getPieceIndex() == torrentInfo.getPieces().size() - 1)
                        blockLength = torrentInfo.getTotalSize() -
                                ((torrentInfo.getPieces().size() - 1) * torrentInfo.getPieceLength());
                    return new PieceMessage(null, null, blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                            toRandomByteArray.apply((int) (blockLength - blockOfPiece.getFrom())));
                });

        TorrentStatusController torrentStatusController =
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo);

        ActiveTorrent activeTorrent = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, fullDownloadPath, torrentStatusController, pieceMessageFlux)
                .block();

        Flux<RequestMessage> assertWrittenPiecesFlux =
                Flux.zip(activeTorrent.savedBlockFlux().autoConnect(), pieceMessageFlux,
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

    @Then("^completed pieces are for torrent: \"([^\"]*)\" in \"([^\"]*)\":$")
    public void completedPiecesAreForTorrent(String torrentFileName,
                                             String downloadLocation,
                                             List<Integer> completedPiecesIndexList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        String fullDownloadPath = System.getProperty("user.dir") + "/" + downloadLocation;
        ActiveTorrent activeTorrent = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .block();

        String errorMessage1 = "the piece is not completed but it should be.";
        String errorMessage2 = "The read operation failed to read exactly what we wrote";

        completedPiecesIndexList.forEach(completedPiecesIndex ->
                Assert.assertTrue(errorMessage1, activeTorrent.havePiece(completedPiecesIndex)));

        // check again in other way: (by ActiveTorrent::buildBitFieldMessage)
        BitFieldMessage allPiecesStatus = activeTorrent.buildBitFieldMessage(null, null);
        completedPiecesIndexList.forEach(completedPiecesIndex ->
                Assert.assertTrue(errorMessage1, allPiecesStatus.getPiecesStatus().get(completedPiecesIndex)));

        // check again in other way: (by ActiveTorrent::buildPieceMessage)

        Flux<PieceMessage> completedPiecesMessageFlux = Flux.fromIterable(completedPiecesIndexList)
                .map(pieceIndex -> new RequestMessage(null, null,
                        pieceIndex,
                        0,
                        activeTorrent.getPieceLength()))
                // if the above piece is not completed, ActiveTorrent::buildPieceMessage will throw exception.
                // but it must complete because the piece is in completedPiecesIndexList list.
                .flatMap(requestMessage -> activeTorrent.buildPieceMessage(requestMessage))
                .doOnNext(pieceMessage -> {
                    RequestMessage requestMessage =
                            new RequestMessage(null, null,
                                    pieceMessage.getIndex(),
                                    pieceMessage.getBegin(),
                                    pieceMessage.getBlock().length);
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestMessage);
                    Assert.assertArrayEquals(errorMessage2, actualWrittenBytes, pieceMessage.getBlock());
                });

        // check that all other pieces are not in complete mode.
        for (int i = 0; i < torrentInfo.getPieces().size(); i++) {
            if (!completedPiecesIndexList.contains(i)) {
                String errorMessage3 = "piece is not completed but it is specified as completed piece: " + i;
                Assert.assertFalse(errorMessage3, allPiecesStatus.getPiecesStatus().get(i));
            }
        }

        StepVerifier.create(completedPiecesMessageFlux)
                .expectNextCount(completedPiecesIndexList.size())
                .verifyComplete();

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);
    }

    @Then("^application save the last piece of torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationSaveAllTheLastPieceOfTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);

        String fullDownloadPath = System.getProperty("user.dir") + "/" + downloadLocation;

        int lastPieceLength = (int) Math.min(torrentInfo.getPieceLength(),
                torrentInfo.getTotalSize() - (torrentInfo.getPieces().size() - 1) * torrentInfo.getPieceLength());

        // generate random complete piece.
        byte[] lastPiece = new byte[lastPieceLength];
        byte content = 0;
        for (int i = 0; i < lastPieceLength; i++, content++)
            lastPiece[i] = content;

        int lastPieceIndex = torrentInfo.getPieces().size() - 1;
        PieceMessage lastPieceMessage = new PieceMessage(null, null, lastPieceIndex, 0, lastPiece);
        RequestMessage requestLastPieceMessage = new RequestMessage(null, null,
                lastPieceMessage.getIndex(),
                lastPieceMessage.getBegin(),
                lastPieceMessage.getBlock().length);

        TorrentStatusController torrentStatusController =
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo);

        ActiveTorrent activeTorrent = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, fullDownloadPath, torrentStatusController, Flux.just(lastPieceMessage))
                .block();

        Mono<PieceMessage> readLastPieceTaskMono = activeTorrent.savedBlockFlux()
                .autoConnect()
                .doOnNext(torrentPieceChanged -> {
                    String message = "the last piece must be completed but it's not.";
                    Assert.assertEquals(message, TorrentPieceStatus.COMPLETED, torrentPieceChanged.getTorrentPieceStatus());
                })
                // assert that we wrote to the file what we should have.
                .doOnNext(torrentPieceChanged -> {
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestLastPieceMessage);
                    String errorMessage = "The read operation failed to read exactly what we wrote";
                    Assert.assertArrayEquals(errorMessage, actualWrittenBytes, lastPieceMessage.getBlock());
                })
                // assert that we can read the last piece successfully.
                .flatMap(torrentPieceChanged -> activeTorrent.buildPieceMessage(requestLastPieceMessage))
                .doOnNext(pieceMessage -> {
                    byte[] actualWrittenBytes = Utils.readFromFile(activeTorrent, fullDownloadPath, requestLastPieceMessage);
                    String errorMessage = "The read operation failed to read exactly what we wrote";
                    Assert.assertArrayEquals(errorMessage, actualWrittenBytes, pieceMessage.getBlock());
                })
                // there must be only one signal here: the last piece. so single() must work.
                .single();

        StepVerifier.create(readLastPieceTaskMono)
                .expectNextCount(1)
                .verifyComplete();

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);
    }

    private SpeedStatistics torrentDownloadSpeedStatistics;

    @Given("^size of incoming messages every \"([^\"]*)\" mill-seconds from a peer:$")
    public void sizeOfIncomingMessagesEveryMillSecondsFromAPeer(int delay, List<Integer> incomingMessageSizeList) {

        Flux<? extends PeerMessage> receivedMessageMessages = Flux.fromIterable(incomingMessageSizeList)
                .delayElements(Duration.ofMillis(delay))
                .map(incomingMessageSize ->
                        new PieceMessage(null, null, 0, 0, new byte[incomingMessageSize]));

        Flux<? extends PeerMessage> sentSentMessages = Flux.empty();

        this.torrentDownloadSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo, receivedMessageMessages, sentSentMessages);
    }

    @Then("^download statistics every 100 mill-seconds are from a peer:$")
    public void downloadStatisticsEveryMillSecondsAreFromAPeer(List<Double> downloadSpeedStatistics) {

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
    public void outgoingMessagesEveryMillSecondsFromAPeer(int delay, List<Integer> outgoingMessageSizeList) {
        Flux<? extends PeerMessage> receivedMessageMessages = Flux.fromIterable(outgoingMessageSizeList)
                .delayElements(Duration.ofMillis(delay))
                .map(outgoingMessageSize ->
                        new PieceMessage(null, null, 0, 0, new byte[outgoingMessageSize]));

        Flux<? extends PeerMessage> sentSentMessages = Flux.empty();
        this.torrentUploadSpeedStatistics =
                new TorrentSpeedSpeedStatisticsImpl(this.torrentInfo, receivedMessageMessages, sentSentMessages);
    }

    @Then("^upload statistics every 100 mill-seconds are from a peer:$")
    public void uploadStatisticsEveryMillSecondsAreFromAPeer(List<Double> uploadSpeedStatistics) {
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
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Utils.removeEverythingRelatedToTorrent(torrentInfo);

        // we won't download anything but we still need to specify a path to download to.
        String DEFAULT_DOWNLOAD_LOCATION = System.getProperty("user.dir") + "/" + "torrents-test/";
        TorrentDownloader torrentDownloader = Utils.createDefaultTorrentDownloader(torrentInfo, DEFAULT_DOWNLOAD_LOCATION);

        // consume new peers and new responses from 1.5 seconds.
        // filter distinct peers from the responses, and assert
        // that both the list of peers are equal.

        Flux<Peer> connectedPeersFlux = torrentDownloader.getPeersCommunicatorFlux()
                .map(PeersCommunicator::getPeer)
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .take(3)
                .flatMap(Flux::fromIterable)
                .sort();

        Flux<Peer> peersFromResponsesMono = torrentDownloader.getPeersCommunicatorFlux()
                .map(PeersCommunicator::receivePeerMessages)
                .flatMap(ReceiveMessages::getPeerMessageResponseFlux)
                .map(PeerMessage::getFrom)
                .distinct()
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .take(2)
                .flatMap(Flux::fromIterable)
                .sort()
                // I'm going to get this peers again AFTER:
                // torrentDownloader.getTorrentStatusController().start();
                .replay()
                .autoConnect();

        // for recording all the peers without blocking the main thread.
        peersFromResponsesMono.subscribe();

        torrentDownloader.getTorrentStatusController().startDownload();
        torrentDownloader.getTorrentStatusController().startUpload();

        List<Peer> connectedPeers = connectedPeersFlux.collectList().block();
        List<Peer> peersFromResponses = peersFromResponsesMono.collectList().block();

        peersFromResponses.stream()
                .filter(peer -> connectedPeers.contains(peer))
                .findFirst()
                .ifPresent(peer -> Assert.fail("We received from the following peer" +
                        " messages but he doesn't exist in the connected peers flux: " + peer));

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);
    }

    @Given("^initial torrent-status for torrent: \"([^\"]*)\" in \"([^\"]*)\" is:$")
    public void activeTorrentForInWithTheFollowingStatus(String torrentFileName, String downloadLocation,
                                                         Map<TorrentStatusType, Boolean> initialTorrentStatusTypeMap) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToTorrent(torrentInfo);

        TorrentStatusController torrentStatusController = new TorrentStatusControllerImpl(torrentInfo,
                initialTorrentStatusTypeMap.get(TorrentStatusType.START_DOWNLOAD),
                initialTorrentStatusTypeMap.get(TorrentStatusType.START_UPLOAD),
                initialTorrentStatusTypeMap.get(TorrentStatusType.REMOVE_TORRENT),
                initialTorrentStatusTypeMap.get(TorrentStatusType.REMOVE_FILES),
                initialTorrentStatusTypeMap.get(TorrentStatusType.RESUME_UPLOAD),
                initialTorrentStatusTypeMap.get(TorrentStatusType.RESUME_DOWNLOAD),
                initialTorrentStatusTypeMap.get(TorrentStatusType.COMPLETED_DOWNLOADING));

        Utils.createDefaultTorrentDownloader(torrentInfo,
                System.getProperty("user.dir") + "/" + downloadLocation,
                torrentStatusController);
    }

    private List<TorrentStatusType> torrentStatusTypeFlux = new ArrayList<>();

    @When("^torrent-status for torrent \"([^\"]*)\" is trying to change to:$")
    public void torrentStatusForIsTryingToChangeTo(String torrentFileName,
                                                   List<TorrentStatusType> changeTorrentStatusTypeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentStatusController torrentStatusController = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getTorrentStatusController();

        Flux<TorrentStatusType> torrentStatusTypeFlux = torrentStatusController.getStatusTypeFlux()
                .replay()
                .autoConnect();
        this.torrentStatusTypeFlux = new ArrayList<>();
        torrentStatusTypeFlux.subscribe(this.torrentStatusTypeFlux::add);

        changeTorrentStatusTypeList.forEach(torrentStatusType -> {
            switch (torrentStatusType) {
                case START_DOWNLOAD:
                    torrentStatusController.startDownload();
                    break;
                case START_UPLOAD:
                    torrentStatusController.startUpload();
                    break;
                case PAUSE_DOWNLOAD:
                    torrentStatusController.pauseDownload();
                    break;
                case RESUME_DOWNLOAD:
                    torrentStatusController.resumeDownload();
                    break;
                case PAUSE_UPLOAD:
                    torrentStatusController.pauseUpload();
                    break;
                case RESUME_UPLOAD:
                    torrentStatusController.resumeUpload();
                    break;
                case COMPLETED_DOWNLOADING:
                    torrentStatusController.completedDownloading();
                    break;
                case REMOVE_TORRENT:
                    torrentStatusController.removeTorrent();
                    break;
                case REMOVE_FILES:
                    torrentStatusController.removeFiles();
                    break;
            }
        });
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be:$")
    public void torrentStatusForWillBe(String torrentFileName,
                                       List<TorrentStatusType> changedTorrentStatusTypeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentStatusController torrentStatusController = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getTorrentStatusController();

        // assert that the state is changed by using methods: this.torrentStatusController.isXXX().
        changedTorrentStatusTypeList.forEach(torrentStatusType -> {
            switch (torrentStatusType) {
                case START_DOWNLOAD:
                    Assert.assertTrue(torrentStatusController.isStartedDownload());
                    break;
                case START_UPLOAD:
                    Assert.assertTrue(torrentStatusController.isStartedUpload());
                    break;
                case PAUSE_DOWNLOAD:
                    Assert.assertFalse(torrentStatusController.isDownloading());
                    break;
                case RESUME_DOWNLOAD:
                    Assert.assertTrue(torrentStatusController.isDownloading());
                    break;
                case PAUSE_UPLOAD:
                    Assert.assertFalse(torrentStatusController.isUploading());
                    break;
                case RESUME_UPLOAD:
                    Assert.assertTrue(torrentStatusController.isUploading());
                    break;
                case COMPLETED_DOWNLOADING:
                    Assert.assertTrue(torrentStatusController.isCompletedDownloading());
                    break;
                case REMOVE_TORRENT:
                    Assert.assertTrue(torrentStatusController.isTorrentRemoved());
                    break;
                case REMOVE_FILES:
                    Assert.assertTrue(torrentStatusController.isFileRemoved());
                    break;
            }
        });

        // assert that we receive the proper signals from this.torrentStatusTypeFlux.
        Assert.assertTrue(new HashSet<>(this.torrentStatusTypeFlux).equals(new HashSet<>(changedTorrentStatusTypeList)));
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be: Empty-table$")
    public void torrentStatusForTorrentWillBeEmptyTable(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentStatusController torrentStatusController = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getTorrentStatusController();

        // assert that we receive the proper signals from this.torrentStatusTypeFlux.
        Assert.assertTrue(new HashSet<>(this.torrentStatusTypeFlux).equals(new HashSet<>(Collections.emptyList())));
    }

    private Mono<List<RemoteFakePeerForRequestingPieces>> requestsFromPeerToMeListMono;
    private Mono<RemoteFakePeerForRequestingPieces> connectionFromFakePeerToMeMono;

    @Then("^random-fake-peer connect to me for torrent: \"([^\"]*)\" in \"([^\"]*)\" and he request:$")
    public void randomFakePeerConnectToMeForTorrentInAndHeRequest(String torrentFileName, String downloadLocation,
                                                                  List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // The last step created ActiveTorrent object which listen to custom
        // peerResponsesFlux. So I can't expect it to react to the original peerResponsesFlux.
        ActiveTorrent activeTorrent = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .block();

        // this flux is empty because if not, the application will get the peers from
        // them and then it will connect to all those peers and then those peers will
        // sendMessage me incoming messages and I don't want any incoming messages but the
        // messages from my fake-peer.
        Flux<TrackerConnection> trackerConnectionFlux = Flux.empty();
        TorrentDownloader torrentDownloader =
                Utils.createCustomTorrentDownloader(torrentInfo, activeTorrent, trackerConnectionFlux);

        // the fake-peer will connect to me.
        Peer me = new Peer("localhost", AppConfig.getInstance().getMyListeningPort());

        // I can't activate this flux until the app start listening for new incoming peers.
        // No one is listening for new peers now so if fake-peer try to connect to me, he will fail.
        // I can't start for new incoming peers in this step because if yes, I will lose all the
        // pieceMessage from this app to the fake-peer and I need them for testing that we did sent them.
        this.requestsFromPeerToMeListMono = torrentDownloader.getPeersProvider()
                // connection of fake-peer to me.
                // later we can get this fake-peer from the flux of connected
                // peers because this flux will contain the incoming peers also.
                .connectToPeerMono(me)
                .map(RemoteFakePeerForRequestingPieces::new)
                .flatMap(RemoteFakePeerForRequestingPieces::sendInterestedMessage)
                // sendMessage all requests from fake peer to me.
                .flatMapMany(fakePeersCommunicator ->
                        Flux.fromIterable(peerRequestBlockList)
                                .flatMap(blockOfPiece -> {
                                    if (blockOfPiece.getLength() != null)
                                        return fakePeersCommunicator.sendRequestMessage(blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                                                blockOfPiece.getLength());
                                    long blockLength = torrentInfo.getPieceLength(blockOfPiece.getPieceIndex());
                                    return fakePeersCommunicator.sendRequestMessage(blockOfPiece.getPieceIndex(),
                                            blockOfPiece.getFrom(), (int) (blockLength - blockOfPiece.getFrom()));
                                }))
                .collectList()
                .doOnNext(requestList -> Assert.assertEquals("We sent less requests then expected.",
                        peerRequestBlockList.size(), requestList.size()));
    }

    @Then("^we assert that for torrent: \"([^\"]*)\", we gave the following pieces to the random-fake-peer:$")
    public void weAssertThatForTorrentWeGaveTheFollowingPiecesToTheRandomFakePeer(String torrentFileName,
                                                                                  List<BlockOfPiece> expectedBlockFromMeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        torrentDownloader.getTorrentStatusController().startUpload();

        // wait until the app will start listen for new incoming peers
        Thread.sleep(500);

        // the fake-peer is the only peer.
        ConnectableFlux<PeersCommunicator> singleFakePeerCommunicatorFlux =
                torrentDownloader.getPeersCommunicatorFlux()
                        .replay();

        // start record the fake-peer-communicator object
        singleFakePeerCommunicatorFlux.connect();

        ConnectableFlux<PieceMessage> sentMessagesFromFakePeerToApplicationFlux =
                singleFakePeerCommunicatorFlux
                        .map(PeersCommunicator::sendMessages)
                        .flatMap(SendPeerMessages::sentPeerMessagesFlux)
                        // the application sendMessage to the fake-peer only PieceMessages in this test.
                        // + my algorithm may sendMessage at the start a bitfield-message.
                        .filter(peerMessage -> peerMessage instanceof PieceMessage)
                        .cast(PieceMessage.class)
                        // if there is a problem, I don't want to wait for ever.
//                .timeout(Duration.ofSeconds(5))
                        .take(expectedBlockFromMeList.size())
                        .replay();

        // start record the messages the app sendMessage to the fake-peer
        // **before** we sending the messages.
        // if I will record only after the messages are sent,
        // I won't get them because no one subscribed to get them.
        sentMessagesFromFakePeerToApplicationFlux.connect();

        List<PieceMessage> actualBlockFromMeList = this.requestsFromPeerToMeListMono
                .flatMapMany(remoteFakePeerForRequestingPieces -> sentMessagesFromFakePeerToApplicationFlux)
                .collectList()
                .block();

        // assert that both the list are equal.

        expectedBlockFromMeList.forEach(blockOfPiece ->
                Assert.assertTrue("the app didn't sendMessage the block: " + blockOfPiece, actualBlockFromMeList.stream()
                        .anyMatch(pieceMessage -> blockOfPiece.getPieceIndex() == pieceMessage.getIndex() &&
                                blockOfPiece.getFrom() == pieceMessage.getBegin() &&
                                blockOfPiece.getLength() == pieceMessage.getBlock().length)));

        actualBlockFromMeList.stream()
                .forEach(pieceMessage ->
                        Assert.assertTrue("the app sendMessage a block which it didn't suppose to sendMessage: " + pieceMessage, expectedBlockFromMeList.stream()
                                .anyMatch(blockOfPiece -> blockOfPiece.getPieceIndex() == pieceMessage.getIndex() &&
                                        blockOfPiece.getFrom() == pieceMessage.getBegin() &&
                                        blockOfPiece.getLength() == pieceMessage.getBlock().length)));

        // the fake-peer is the only peer.
        singleFakePeerCommunicatorFlux
                .subscribe(x -> x.closeConnection());

        Utils.removeEverythingRelatedToTorrent(torrentInfo);
    }
}

