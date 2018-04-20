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
import main.downloader.TorrentPieceChanged;
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
import main.torrent.status.TorrentStatus;
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

        List<PeerMessageType> messageToSendList = peerFakeRequestResponses.stream()
                .map(PeerFakeRequestResponse::getSendMessageType)
                .collect(Collectors.toList());

        // check if we expect an error signal.
        Optional<ErrorSignalType> errorSignalTypeOptional = peerFakeRequestResponses.stream()
                .filter(peerFakeRequestResponse -> peerFakeRequestResponse.getErrorSignalType() != null)
                .map(PeerFakeRequestResponse::getErrorSignalType)
                .findAny();

        // check if we expect a complete signal
        Optional<PeerFakeRequestResponse> completeSignalOptional = peerFakeRequestResponses.stream()
                .filter(peerFakeRequestResponse -> peerFakeRequestResponse.getErrorSignalType() == null &&
                        peerFakeRequestResponse.getReceiveMessageType() == null)
                .findAny();

        boolean expectResponseToEveryRequest = peerFakeRequestResponses.stream()
                .allMatch(peerFakeRequestResponse -> peerFakeRequestResponse.getErrorSignalType() == null &&
                        peerFakeRequestResponse.getReceiveMessageType() != null);

        Link fakePeer = peersProvider.connectToPeerMono(remoteFakePeerCopyCat)
                .block();

        Flux<? extends PeerMessage> recordedResponseFlux =
                Flux.fromIterable(messageToSendList)
                        .flatMap(peerMessageType ->
                                Utils.getSpecificMessageResponseFluxByMessageType(fakePeer, peerMessageType))
                        .replay()
                        // start record incoming messages from fake peer
                        .autoConnect(0);


        Mono<List<SendPeerMessages>> sentMessagesMono = Flux.fromIterable(messageToSendList)
                .flatMap(peerMessageType ->
                        Utils.sendFakeMessage(fakePeer, peerMessageType))
                .collectList();

        if (expectResponseToEveryRequest)
            StepVerifier
                    .create(sentMessagesMono
                            .flatMapMany(peersCommunicator -> recordedResponseFlux)
                            .take(messageToSendList.size()))
                    .expectNextCount(messageToSendList.size())
                    .verifyComplete();

        errorSignalTypeOptional.map(ErrorSignalType::getErrorSignal)
                .ifPresent(errorSignalType ->
                        StepVerifier
                                .create(sentMessagesMono
                                        .flatMapMany(peersCommunicator -> recordedResponseFlux)
                                        .take(messageToSendList.size() - 1))
                                .expectNextCount(messageToSendList.size() - 1)
                                .expectError(errorSignalType)
                                .verify());

        completeSignalOptional.map(PeerFakeRequestResponse::getSendMessageType)
                .ifPresent(errorSignalType1 ->
                        StepVerifier
                                .create(sentMessagesMono
                                        .flatMapMany(peersCommunicator -> recordedResponseFlux)
                                        .take(messageToSendList.size() - 1))
                                .expectNextCount(messageToSendList.size() - 1)
                                .verifyComplete());

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

        int requestBlockSize = 16_384;

        Function<BitFieldMessage, List<Integer>> getCompletedPieces = bitFieldMessage -> {
            List<Integer> completedPieces = new ArrayList<>();
            for (int i = 0; i < bitFieldMessage.getPiecesStatus().size(); i++)
                if (bitFieldMessage.getPiecesStatus().get(i))
                    completedPieces.add(i);
            return completedPieces;
        };

        Mono<PieceMessage> receiveSinglePieceMono =
                torrentDownloader.getPeersCommunicatorFlux()
                        .replay()
                        .autoConnect(0)
                        .flatMap(peersCommunicator -> peersCommunicator.sendMessages()
                                .sendInterestedMessage()
                                .map(sendPeerMessages -> peersCommunicator))
                        .flatMap(peersCommunicator ->
                                peersCommunicator.receivePeerMessages()
                                        .getBitFieldMessageResponseFlux()
                                        .map(bitFieldMessage -> getCompletedPieces.apply(bitFieldMessage))
                                        .flatMap(Flux::fromIterable)
                                        .take(5)
                                        .flatMap(completedPieceIndex -> peersCommunicator.sendMessages()
                                                .sendRequestMessage(completedPieceIndex, 0, requestBlockSize)
                                                .map(sendPeerMessages -> peersCommunicator)))
                        .flatMap(peersCommunicator ->
                                peersCommunicator.receivePeerMessages()
                                        .getPieceMessageResponseFlux()
                                        .doOnNext(pieceMessage -> peersCommunicator.closeConnection()))
                        .take(1)
                        .single();

        torrentDownloader.getTorrentStatusController().startUpload();

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
                })
                .publish()
                // when ActiveTorrent class and me in the test will subscribe, then start signaling.
                // if I remove this, then in the tests I will lose all the signals this class will send in
                // activeTorrent.savedBlockFlux().
                .autoConnect(3);

        TorrentStatusController torrentStatusController =
                TorrentStatusControllerImpl.createDefaultTorrentStatusController(torrentInfo);

        ActiveTorrent activeTorrent = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, fullDownloadPath, torrentStatusController, pieceMessageFlux)
                .block();

        Flux<TorrentPieceChanged> recordedTorrentPieceChangedFlux =
                activeTorrent.savedBlockFlux()
                        .replay()
                        .autoConnect(0);

        // will cause ActiveTorrent to start recording signals in activeTorrent.savedBlockFlux().
        pieceMessageFlux.subscribe();

        Flux<RequestMessage> assertWrittenPiecesFlux =
                Flux.zip(recordedTorrentPieceChangedFlux, pieceMessageFlux,
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

        Mono<PieceMessage> readLastPieceTaskMono =
                activeTorrent.savedBlockFlux()
                        .replay()
                        .autoConnect(0)
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
                        .take(1)
                        .single();

        torrentStatusController.startDownload();

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
                .map(Link::getPeer)
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .take(3)
                .flatMap(Flux::fromIterable)
                .sort();

        Flux<Peer> peersFromResponsesMono = torrentDownloader.getPeersCommunicatorFlux()
                .map(Link::receivePeerMessages)
                .flatMap(ReceivePeerMessages::getPeerMessageResponseFlux)
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

        TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo, null, null,
                        torrentStatusController, null, null, null,
                        null, null);

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

        Flux<TorrentStatusType> torrentStatusTypeFlux =
                torrentStatusController.getStatusTypeFlux()
                        .replay()
                        .autoConnect(0);
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
        TorrentStatus torrentStatus = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getTorrentStatusController();

        // assert that the state is changed.

        boolean expectedStartDownload = changedTorrentStatusTypeList.contains(TorrentStatusType.START_DOWNLOAD);
        Assert.assertEquals(expectedStartDownload, torrentStatus.isStartedDownloadingFlux()
                .filter(isTrue -> isTrue == expectedStartDownload)
                .blockFirst());

        boolean expectedStartUpload = changedTorrentStatusTypeList.contains(TorrentStatusType.START_UPLOAD);
        Assert.assertEquals(expectedStartUpload, torrentStatus.isStartedUploadingFlux()
                .filter(isTrue -> isTrue == expectedStartUpload)
                .blockFirst());

        boolean expectedDownloading = changedTorrentStatusTypeList.contains(TorrentStatusType.RESUME_DOWNLOAD);
        Assert.assertEquals(expectedDownloading, torrentStatus.isDownloadingFlux()
                .filter(isTrue -> isTrue == expectedDownloading)
                .blockFirst());

        boolean expectedUploading = changedTorrentStatusTypeList.contains(TorrentStatusType.RESUME_UPLOAD);
        Assert.assertEquals(expectedUploading, torrentStatus.isUploadingFlux()
                .filter(isTrue -> isTrue == expectedUploading)
                .blockFirst());

        boolean expectedCompletedDownload = changedTorrentStatusTypeList.contains(TorrentStatusType.COMPLETED_DOWNLOADING);
        Assert.assertEquals(expectedCompletedDownload, torrentStatus.isCompletedDownloadingFlux()
                .filter(isTrue -> isTrue == expectedCompletedDownload)
                .blockFirst());

        boolean expectedRemovedFiles = changedTorrentStatusTypeList.contains(TorrentStatusType.REMOVE_FILES);
        Assert.assertEquals(expectedRemovedFiles, torrentStatus.isFilesRemovedFlux()
                .filter(isTrue -> isTrue == expectedRemovedFiles)
                .blockFirst());

        boolean expectedRemoveTorrent = changedTorrentStatusTypeList.contains(TorrentStatusType.REMOVE_TORRENT);
        Assert.assertEquals(expectedRemoveTorrent, torrentStatus.isTorrentRemovedFlux()
                .filter(isTrue -> isTrue == expectedRemoveTorrent)
                .blockFirst());
    }

    private Mono<Link> meToFakePeerLink;
    private Mono<List<SendPeerMessages>> requestsFromPeerToMeListMono;

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
        // represent this application TorrentDownloader. (not the fake-peer TorrentDownloader).
        TorrentDownloader torrentDownloader =
                Utils.createCustomTorrentDownloader(torrentInfo, activeTorrent, trackerConnectionFlux);

        // the fake-peer will connect to me.
        Peer me = new Peer("localhost", AppConfig.getInstance().getMyListeningPort());

        this.meToFakePeerLink = torrentDownloader.getPeersCommunicatorFlux()
                .replay()
                .autoConnect(0)
                .take(1)
                .single();

        // my application start listening for new peers.
        // (it will happen only when I'm starting download or upload).
        // and also listen for incoming request messages and response to them.
        torrentDownloader.getTorrentStatusController().startUpload();

        // wait until the app will start listen for new incoming peers
        Thread.sleep(500);

        Link fakePeerToMeLink = new PeersProvider(torrentInfo)
                // fake-peer connect to me.
                .connectToPeerMono(me)
                .block();

        this.requestsFromPeerToMeListMono = fakePeerToMeLink.sendMessages().sendInterestedMessage()
                // sendMessage all requests from fake peer to me.
                .flatMapMany(__ -> Flux.fromIterable(peerRequestBlockList))
                .flatMap(blockOfPiece -> {
                    if (blockOfPiece.getLength() != null)
                        return fakePeerToMeLink.sendMessages().sendRequestMessage(blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(),
                                blockOfPiece.getLength());
                    long blockLength = torrentInfo.getPieceLength(blockOfPiece.getPieceIndex());
                    return fakePeerToMeLink.sendMessages().sendRequestMessage(blockOfPiece.getPieceIndex(),
                            blockOfPiece.getFrom(), (int) (blockLength - blockOfPiece.getFrom()));
                })
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

        // I must record this because when I subscribe to this.requestsFromPeerToMeListMono,
        // fake-peer will send me request messages and I response to him **piece messages**
        // which I don't want to lose.
        Flux<PieceMessage> recordedPieceMessageFlux = this.meToFakePeerLink
                .map(Link::sendMessages)
                .flatMapMany(SendPeerMessages::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class)
                .replay()
                .autoConnect(0);

        // send request massages from fake peer to me and get all the
        // piece messages from me to fake peer and collect them to list.
        List<PieceMessage> actualBlockFromMeList = this.requestsFromPeerToMeListMono
                .flatMapMany(remoteFakePeerForRequestingPieces -> recordedPieceMessageFlux)
                .take(expectedBlockFromMeList.size())
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

        meToFakePeerLink.subscribe(Link::closeConnection);

        Utils.removeEverythingRelatedToTorrent(torrentInfo);
    }
}

