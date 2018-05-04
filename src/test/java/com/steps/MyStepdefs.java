package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.AppConfig;
import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.*;
import main.peer.*;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.Status;
import main.torrent.status.StatusChanger;
import main.torrent.status.StatusType;
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
        Hooks.onErrorDropped(throwable -> {
        });

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
        Utils.removeEverythingRelatedToLastTest();

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

    @Then("^application send to \\[peer ip: \"([^\"]*)\", peer port: \"([^\"]*)\"] and receive the following messages for torrent: \"([^\"]*)\":$")
    public void applicationSendToPeerIpPeerPortAndReceiveTheFollowingMessagesForTorrent(String peerIp, int peerPort, String torrentFileName,
                                                                                        List<PeerFakeRequestResponse> peerFakeRequestResponses) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        RemoteFakePeerCopyCat remoteFakePeerCopyCat = new RemoteFakePeerCopyCat(torrentInfo, new Peer(peerIp, peerPort));
        remoteFakePeerCopyCat.listen();

        PeersProvider peersProvider = new PeersProvider(torrentInfo);

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
                        .flatMap(peerMessageType -> Utils.getSpecificMessageResponseFluxByMessageType(fakePeer, peerMessageType))
                        .replay()
                        // start record incoming messages from fake peer
                        .autoConnect(0);


        Mono<List<SendMessagesNotifications>> sentMessagesMono = Flux.fromIterable(messageToSendList)
                .flatMap(peerMessageType ->
                        Utils.sendFakeMessage(fakePeer, peerMessageType))
                .collectList();

        if (expectResponseToEveryRequest)
            StepVerifier.create(sentMessagesMono
                    .flatMapMany(peersCommunicator -> recordedResponseFlux)
                    .take(messageToSendList.size()))
                    .expectNextCount(messageToSendList.size())
                    .verifyComplete();

        errorSignalTypeOptional.map(ErrorSignalType::getErrorSignal)
                .ifPresent(errorSignalType ->
                        StepVerifier.create(sentMessagesMono
                                .flatMapMany(peersCommunicator -> recordedResponseFlux)
                                .take(messageToSendList.size() - 1))
                                .expectNextCount(messageToSendList.size() - 1)
                                .expectError(errorSignalType)
                                .verify());

        completeSignalOptional.map(PeerFakeRequestResponse::getSendMessageType)
                .ifPresent(errorSignalType1 ->
                        StepVerifier.create(sentMessagesMono
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

        torrentDownloader.getStatusChanger().changeStatus(StatusType.START_UPLOAD).block();

        StepVerifier.create(receiveSinglePieceMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^application create active-torrent for: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationCreateActiveTorrentFor(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        // this will create an activeTorrent object.
        Utils.createDefaultTorrentDownloader(torrentInfo,
                System.getProperty("user.dir") + File.separator + downloadLocation + File.separator);
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Mono<Optional<FileSystemLink>> activeTorrentMono =
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
                System.getProperty("user.dir") + File.separator + downloadLocation + File.separator + torrentInfo.getName() + File.separator :
                System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;
        List<String> filePathList = torrentInfo.getFileList()
                .stream()
                .map(TorrentFile::getFileDirs)
                .map(List::stream)
                .map((Stream<String> incompleteFilePath) ->
                        incompleteFilePath.collect(Collectors.joining(File.separator, fullFilePath, "")))
                .collect(Collectors.toList());

        if (torrentFilesExist) {
            String mainFilePath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator + torrentInfo.getName() + File.separator;
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
                        // TODO: we create a sparse file so it doesn't have an initial length.
//						Assert.assertEquals("file not in the right length: " + file.getPath(),
//								(long) torrentFile.getFileLength(), file.length());
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

        FileSystemLink fileSystemLink = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getFileSystemLink();

        Mono<FileSystemLink> deletionTaskMono;
        if (deleteTorrentFiles && deleteActiveTorrent)
            deletionTaskMono = fileSystemLink.deleteFileOnlyMono()
                    .flatMap(activeTorrent -> activeTorrent.deleteActiveTorrentOnlyMono());
        else if (deleteTorrentFiles)
            deletionTaskMono = fileSystemLink.deleteFileOnlyMono();
        else // deleteActiveTorrent == true
            deletionTaskMono = fileSystemLink.deleteActiveTorrentOnlyMono();

        StepVerifier.create(deletionTaskMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    // TODO: this test does not have good design because it collects all the pieceMessages to multiple sets so we can't
    // free the allocatedBlocks objects. We may run in a situation when each block is 1gb so we will get out of memory exception.
    @When("^application save random blocks for torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Flux<PieceMessage> pieceMessagesFlux = Flux.fromIterable(blockList)
                .flatMap((BlockOfPiece blockOfPiece) -> Utils.createRandomPieceMessages(torrentInfo, blockOfPiece,
                        BlocksAllocatorImpl.getInstance().getBlockLength()))
                .publish()
                .autoConnect(2);

        StatusChanger statusChanger = new StatusChanger(new Status(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false));

        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;
        FileSystemLink fileSystemLink = ActiveTorrents.getInstance()
                .createActiveTorrentMono(torrentInfo, fullDownloadPath, statusChanger, pieceMessagesFlux)
                .block();

        Flux<PieceEvent> recordedTorrentPieceChangedFlux = fileSystemLink
                .savedBlockFlux()
                .replay()
                .autoConnect(0);

        // this subscription to pieceMessagesFlux flux will cause FileSystemLinkImpl
        // to start recording signals in activeTorrent.savedBlockFlux().
        Set<PieceMessage> expectedSavedPieces = pieceMessagesFlux
                .collect(Collectors.toSet())
                .block();

        Set<PieceMessage> actualSavedPiecesFromFileSystem = recordedTorrentPieceChangedFlux
                .map(PieceEvent::getReceivedPiece)
                .map(pieceMessage -> new RequestMessage(null, null, pieceMessage.getIndex(),
                        pieceMessage.getBegin(), pieceMessage.getBlockLength()))
                .map(requestMessage -> RequestMessage.fixRequestMessage(requestMessage, torrentInfo.getPieceLength(requestMessage.getIndex())))
                // I must change the thread because I'm going to block it in readFromFile
                // and reactor doesn't allow to block single or parallel threads.
                .publishOn(Schedulers.elastic())
                .map(requestMessage -> {
                    AllocatedBlock actualWrittenBytes = Utils.readFromFile(torrentInfo, fullDownloadPath, requestMessage);
                    PieceMessage pieceMessage = new PieceMessage(null, null, requestMessage.getIndex(),
                            requestMessage.getBegin(), actualWrittenBytes.getActualLength(), actualWrittenBytes);
                    return PieceMessage.fixPieceMessage(pieceMessage, fileSystemLink.getTorrentInfo().getPieceLength(pieceMessage.getIndex()));
                })
                .collect(Collectors.toSet())
                .block();

        Assert.assertEquals("the pieces we read from filesystem are not equal to the pieces we tried to save to the filesystem.",
                expectedSavedPieces, actualSavedPiecesFromFileSystem);

        Set<PieceMessage> actualSavedPiecesFromEvents = recordedTorrentPieceChangedFlux
                .map(PieceEvent::getReceivedPiece)
                .take(expectedSavedPieces.size())
                .collect(Collectors.toSet())
                .block();

        Set<PieceMessage> expectedCompletedSavedPieces = expectedSavedPieces.stream()
                .filter(pieceMessage -> blockList.stream()
                        .anyMatch(blockOfPiece -> {
                            if (blockOfPiece.getPieceIndex() >= 0)
                                if (blockOfPiece.getPieceIndex() == pieceMessage.getIndex()
                                        && blockOfPiece.getLength() == null)
                                    return true;
                                else if (torrentInfo.getPieces().size() + blockOfPiece.getPieceIndex() == pieceMessage.getIndex()
                                        && blockOfPiece.getLength() == null)
                                    return true;
                            return false;
                        }))
                .collect(Collectors.toSet());

        Set<PieceMessage> actualCompletedSavedPiecesReadByFileSystem = Flux.fromIterable(expectedCompletedSavedPieces)
                .map(pieceMessage -> new RequestMessage(null, null, pieceMessage.getIndex(),
                        pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getActualLength()))
                .map(requestMessage -> RequestMessage.fixRequestMessage(requestMessage, torrentInfo.getPieceLength(requestMessage.getIndex())))
                .flatMap((RequestMessage requestMessage) -> BlocksAllocatorImpl.getInstance()
                        .allocate(0, requestMessage.getBlockLength())
                        .flatMap(allocatedBlock -> fileSystemLink.buildPieceMessage(requestMessage, allocatedBlock)))
                .collect(Collectors.toSet())
                .block();


        Assert.assertEquals("the filesystem module tells that he saved different pieces.",
                expectedSavedPieces, actualSavedPiecesFromEvents);
        Assert.assertEquals("the filesystem module read other completed pieces than the completed pieces we saved by using this filesystem module.",
                expectedCompletedSavedPieces, actualCompletedSavedPiecesReadByFileSystem);

        // I must create it here because later I need to get the torrentStatusController which was already created here.
        // If I'm not creating TorrentDownloader object here, I will create 2 different torrentStatusController objects.
        TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        fileSystemLink,
                        null,
                        statusChanger,
                        null,
                        null,
                        null,
                        null,
                        null);
    }

    @Then("^the only completed pieces are - for torrent: \"([^\"]*)\":$")
    public void completedPiecesAreForTorrent(String torrentFileName,
                                             List<Integer> completedPiecesIndexList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        List<Integer> fixedCompletedPiecesIndexList = completedPiecesIndexList.stream()
                .map(pieceIndex -> pieceIndex >= 0 ?
                        pieceIndex :
                        torrentInfo.getPieces().size() + pieceIndex)
                .collect(Collectors.toList());

        FileSystemLink fileSystemLinkImplTorrent = ActiveTorrents.getInstance()
                .findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .block();

        String errorMessage1 = "the piece is not completed but it should be.";

        fixedCompletedPiecesIndexList.forEach(completedPiecesIndex ->
                Assert.assertTrue(errorMessage1, fileSystemLinkImplTorrent.havePiece(completedPiecesIndex)));

        // check again in other way: (by FileSystemLinkImpl::buildBitFieldMessage)
        BitFieldMessage allPiecesStatus = fileSystemLinkImplTorrent.buildBitFieldMessage(null, null);
        fixedCompletedPiecesIndexList.forEach(completedPiecesIndex ->
                Assert.assertTrue(errorMessage1, allPiecesStatus.getPiecesStatus().get(completedPiecesIndex)));

        // check that all other pieces are not in complete mode.
        for (int i = 0; i < torrentInfo.getPieces().size(); i++) {
            if (!fixedCompletedPiecesIndexList.contains(i)) {
                String errorMessage3 = "piece is not completed but it is specified as completed piece: " + i;
                Assert.assertFalse(errorMessage3, allPiecesStatus.getPiecesStatus().get(i));
            }
        }

        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();
    }

    @Then("^application connect to all peers and assert that we connected to them - for torrent: \"([^\"]*)\"$")
    public void applicationConnectToAllPeersAndAssertThatWeConnectedToThemForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Utils.removeEverythingRelatedToLastTest();

        // we won't download anything but we still need to specify a path to download to.
        String DEFAULT_DOWNLOAD_LOCATION = System.getProperty("user.dir") + File.separator + "torrents-test/";
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
                .flatMap(ReceiveMessagesNotifications::getPeerMessageResponseFlux)
                .map(PeerMessage::getFrom)
                .distinct()
                .timeout(Duration.ofMillis(1500))
                .buffer(Duration.ofMillis(1500))
                .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
                .take(2)
                .flatMap(Flux::fromIterable)
                .sort()
                // I'm going to get this peers again AFTER:
                // torrentDownloader.getStatusChanger().start();
                .replay()
                .autoConnect();

        // for recording all the peers without blocking the main thread.
        peersFromResponsesMono.subscribe();

        torrentDownloader.getStatusChanger().changeStatus(StatusType.START_DOWNLOAD).block();
        torrentDownloader.getStatusChanger().changeStatus(StatusType.START_UPLOAD).block();

        List<Peer> connectedPeers = connectedPeersFlux.collectList().block();
        List<Peer> peersFromResponses = peersFromResponsesMono.collectList().block();

        peersFromResponses.stream()
                .filter(peer -> connectedPeers.contains(peer))
                .findFirst()
                .ifPresent(peer -> Assert.fail("We received from the following peer" +
                        " messages but he doesn't exist in the connected peers flux: " + peer));

        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();
    }

    @Given("^initial torrent-status for torrent: \"([^\"]*)\" in \"([^\"]*)\" is:$")
    public void activeTorrentForInWithTheFollowingStatus(String torrentFileName, String downloadLocation,
                                                         Map<StatusType, Boolean> initialTorrentStatusTypeMap) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        Status initialStatus = new Status(
                initialTorrentStatusTypeMap.get(StatusType.START_DOWNLOAD),
                initialTorrentStatusTypeMap.get(StatusType.START_UPLOAD),
                initialTorrentStatusTypeMap.get(StatusType.REMOVE_TORRENT),
                initialTorrentStatusTypeMap.get(StatusType.REMOVE_FILES),
                initialTorrentStatusTypeMap.get(StatusType.RESUME_UPLOAD),
                initialTorrentStatusTypeMap.get(StatusType.RESUME_DOWNLOAD),
                initialTorrentStatusTypeMap.get(StatusType.COMPLETED_DOWNLOADING),
                initialTorrentStatusTypeMap.get(StatusType.START_LISTENING_TO_INCOMING_PEERS),
                initialTorrentStatusTypeMap.get(StatusType.RESUME_LISTENING_TO_INCOMING_PEERS),
                initialTorrentStatusTypeMap.get(StatusType.START_SEARCHING_PEERS),
                initialTorrentStatusTypeMap.get(StatusType.RESUME_SEARCHING_PEERS));

        TorrentDownloaders.getInstance()
                .createTorrentDownloader(torrentInfo,
                        null,
                        null,
                        new StatusChanger(initialStatus),
                        null,
                        null,
                        null,
                        null,
                        null);
    }

    private Status actualLastStatus = null;

    @When("^torrent-status for torrent \"([^\"]*)\" is trying to change to:$")
    public void torrentStatusForIsTryingToChangeTo(String torrentFileName,
                                                   List<StatusType> changeStatusTypeList) throws Throwable {
        // remove every state from the last test.
        this.actualLastStatus = null;

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        StatusChanger statusChanger = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getStatusChanger();

        changeStatusTypeList.forEach(torrentStatusType -> {
            switch (torrentStatusType) {
                case START_DOWNLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.START_DOWNLOAD).block();
                    break;
                case START_UPLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.START_UPLOAD).block();
                    break;
                case PAUSE_DOWNLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.PAUSE_DOWNLOAD).block();
                    break;
                case RESUME_DOWNLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.RESUME_DOWNLOAD).block();
                    break;
                case PAUSE_UPLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.PAUSE_UPLOAD).block();
                    break;
                case RESUME_UPLOAD:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.RESUME_UPLOAD).block();
                    break;
                case COMPLETED_DOWNLOADING:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.COMPLETED_DOWNLOADING).block();
                    break;
                case REMOVE_TORRENT:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.REMOVE_TORRENT).block();
                    break;
                case REMOVE_FILES:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.REMOVE_FILES).block();
                    break;
                case START_LISTENING_TO_INCOMING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.START_LISTENING_TO_INCOMING_PEERS).block();
                    break;
                case RESUME_LISTENING_TO_INCOMING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.RESUME_LISTENING_TO_INCOMING_PEERS).block();
                    break;
                case PAUSE_LISTENING_TO_INCOMING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.PAUSE_LISTENING_TO_INCOMING_PEERS).block();
                    break;
                case START_SEARCHING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.START_SEARCHING_PEERS).block();
                    break;
                case RESUME_SEARCHING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.RESUME_SEARCHING_PEERS).block();
                    break;
                case PAUSE_SEARCHING_PEERS:
                    this.actualLastStatus = statusChanger.changeStatus(StatusType.PAUSE_SEARCHING_PEERS).block();
                    break;
            }
        });
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be:$")
    public void torrentStatusForWillBe(String torrentFileName,
                                       List<StatusType> expectedStatusTypeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        StatusChanger statusChanger = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get()
                .getStatusChanger();

        Status expectedFinalStatus = new Status(
                expectedStatusTypeList.contains(StatusType.START_DOWNLOAD),
                expectedStatusTypeList.contains(StatusType.START_UPLOAD),
                expectedStatusTypeList.contains(StatusType.REMOVE_TORRENT),
                expectedStatusTypeList.contains(StatusType.REMOVE_FILES),
                expectedStatusTypeList.contains(StatusType.RESUME_UPLOAD),
                expectedStatusTypeList.contains(StatusType.RESUME_DOWNLOAD),
                expectedStatusTypeList.contains(StatusType.COMPLETED_DOWNLOADING),
                expectedStatusTypeList.contains(StatusType.START_LISTENING_TO_INCOMING_PEERS),
                expectedStatusTypeList.contains(StatusType.RESUME_LISTENING_TO_INCOMING_PEERS),
                expectedStatusTypeList.contains(StatusType.START_SEARCHING_PEERS),
                expectedStatusTypeList.contains(StatusType.RESUME_SEARCHING_PEERS));

        // test with the status we received from the "last-status-mono"
        Assert.assertEquals(expectedFinalStatus, statusChanger.getLatestStatus$().block());
        // test with the actual last status we received in the last time we tried to change the status
        if (this.actualLastStatus != null)
            Assert.assertEquals(expectedFinalStatus, this.actualLastStatus);
        this.actualLastStatus = null;
    }

    private Mono<Link> meToFakePeerLink;
    private Mono<List<SendMessagesNotifications>> requestsFromFakePeerToMeListMono;

    @Then("^random-fake-peer connect to me for torrent: \"([^\"]*)\" in \"([^\"]*)\" and he request:$")
    public void randomFakePeerConnectToMeForTorrentInAndHeRequest(String torrentFileName, String downloadLocation,
                                                                  List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // The last step created FileSystemLinkImpl object which listen to custom
        // peerResponsesFlux. So I can't expect it to react to the original peerResponsesFlux.
        // Also the last test created torrentStatusController object.
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        StatusChanger statusChanger = torrentDownloader.getStatusChanger();

        FileSystemLink fileSystemLink = torrentDownloader.getFileSystemLink();

        // this flux is empty because if not, the application will get the peers from
        // them and then it will connect to all those peers and then those peers will
        // sendMessage me incoming messages and I don't want any incoming messages but the
        // messages from my fake-peer.
        Flux<TrackerConnection> trackerConnectionFlux = Flux.empty();

        TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        // represent this application TorrentDownloader. (not the fake-peer TorrentDownloader).
        TorrentDownloader torrentDownloaderNew =
                Utils.createCustomTorrentDownloader(torrentInfo, statusChanger,
                        fileSystemLink, trackerConnectionFlux);

        // the fake-peer will connect to me.
        Peer me = new Peer("localhost", AppConfig.getInstance().getMyListeningPort());

        this.meToFakePeerLink = torrentDownloaderNew.getPeersCommunicatorFlux()
                .replay()
                .autoConnect(0)
                .take(1)
                .single();

        // my application start listening for new peers.
        statusChanger.changeStatus(StatusType.START_LISTENING_TO_INCOMING_PEERS).block();

        // listen for incoming request messages and response to them.
        statusChanger.changeStatus(StatusType.START_UPLOAD).block();

        // wait until the app will start listen for new incoming peers
        // and wait until torrent-status signal "RESUME_LISTENING_TO_INCOMING_PEERS".
        Thread.sleep(500);

        Link fakePeerToMeLink = new PeersProvider(torrentInfo)
                // fake-peer connect to me.
                .connectToPeerMono(me)
                .block();

        this.requestsFromFakePeerToMeListMono = fakePeerToMeLink.sendMessages().sendInterestedMessage()
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
                .doOnNext(requestList -> {
                    Assert.assertEquals("We sent less requests then expected.",
                            peerRequestBlockList.size(), requestList.size());
                });
    }

    @Then("^we assert that for torrent: \"([^\"]*)\", we gave the following pieces to the random-fake-peer:$")
    public void weAssertThatForTorrentWeGaveTheFollowingPiecesToTheRandomFakePeer(String torrentFileName,
                                                                                  List<BlockOfPiece> expectedBlockFromMeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        // I must record this because when I subscribe to this.requestsFromFakePeerToMeListMono,
        // fake-peer will send me request messages and I response to him **piece messages**
        // which I don't want to lose.
        Flux<PieceMessage> recordedPieceMessageFlux = this.meToFakePeerLink
                .map(Link::sendMessages)
                .flatMapMany(SendMessagesNotifications::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class)
                .replay()
                .autoConnect(0);

        // send request massages from fake peer to me and get all the
        // piece messages from me to fake peer and collect them to list.
        Set<BlockOfPiece> actualBlockFromMeSet = this.requestsFromFakePeerToMeListMono
                .flatMapMany(remoteFakePeerForRequestingPieces -> recordedPieceMessageFlux)
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getBlockLength()))
                .take(expectedBlockFromMeList.size())
                .collect(Collectors.toSet())
                .block();

        Set<BlockOfPiece> expectedBlockFromMeSet = expectedBlockFromMeList.stream()
                .collect(Collectors.toSet());

        // assert that both the list are equal.
        Assert.assertEquals(expectedBlockFromMeSet, actualBlockFromMeSet);

        meToFakePeerLink.subscribe(Link::closeConnection);

        Utils.removeEverythingRelatedToLastTest();
    }

    @Given("^torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void torrent(String torrentFileName, String downloadLocation) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        // close the connection of all the fake peers from the last test and clear their map.
        this.fakePeersToMeLinkMap.clear();
        // we can close our connection instead of closing theirs. its the same.
        if (this.recordedMeToFakePeersLinks$ != null) {
            this.recordedMeToFakePeersLinks$
                    .doOnNext(Link::closeConnection)
                    .blockLast(Duration.ofMillis(500));
        }
        this.recordedFakePeersToMeLinks$ = null;
        this.actualSavedBlocks$ = null;

        Flux<TrackerConnection> trackers$ = Flux.empty();
        Utils.createDefaultTorrentDownloader(torrentInfo,
                System.getProperty("user.dir") + File.separator + downloadLocation + File.separator, trackers$);
    }

    private Flux<Link> recordedMeToFakePeersLinks$;
    private Flux<RemoteFakePeer> recordedFakePeersToMeLinks$;
    private Map<Integer, Mono<RemoteFakePeer>> fakePeersToMeLinkMap = new HashMap<>();

    @Given("^link to \"([^\"]*)\" - fake-peer on port \"([^\"]*)\" with the following pieces - for torrent: \"([^\"]*)\"$")
    public void linkToFakePeerWithTheFollowingPiecesForTorrent(FakePeerType fakePeerType,
                                                               Integer fakePeerPort,
                                                               String torrentFileName,
                                                               List<Integer> fakePeerCompletedPieces) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        this.recordedMeToFakePeersLinks$ = torrentDownloader.getPeersCommunicatorFlux()
                // send interested message to the fake-peer.
                .flatMap(link -> link.sendMessages().sendInterestedMessage()
                        .map(sendPeerMessages -> link))
                .replay()
                .autoConnect(0);

        Peer me = new Peer("localhost", AppConfig.getInstance().getMyListeningPort());
        Peer fakePeer = new Peer("localhost", fakePeerPort);

        // build a bitfield message so I can send it to my app and also
        // the fake-peer will update his status that he have those pieces.
        BitSet bitSet = new BitSet(torrentInfo.getPieces().size());
        fakePeerCompletedPieces.stream()
                .map(completedPieceIndex -> completedPieceIndex >= 0 ?
                        completedPieceIndex :
                        torrentInfo.getPieces().size() + completedPieceIndex)
                .forEach(completedPieceIndex -> bitSet.set(completedPieceIndex));

        Mono<RemoteFakePeer> fakePeerToMeLink$ = new PeersProvider(torrentInfo)
                .connectToPeerMono(me)
                .map(link -> new RemoteFakePeer(link, fakePeerType))
                .flatMap(remoteFakePeer -> remoteFakePeer.sendMessages().sendBitFieldMessage(bitSet)
                        .map(sendPeerMessages -> remoteFakePeer));

        this.fakePeersToMeLinkMap.put(fakePeerPort, fakePeerToMeLink$);
    }

    private Flux<PieceEvent> actualSavedBlocks$;

    @When("^application request the following blocks from him - for torrent: \"([^\"]*)\":$")
    public void applicationRequestTheFollowingBlocksFromHimForTorrent(String torrentFileName,
                                                                      List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        // at this point, we already started listening for incoming links from the fake-peers.

        // TODO: un comment this.
//		TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
//		TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
//				.findTorrentDownloader(torrentInfo.getTorrentInfoHash())
//				.get();
//
//		// all fake peers connect to me. In this test there must be only one fake-peer.
//		this.recordedFakePeersToMeLinks$ = Flux.fromIterable(this.fakePeersToMeLinkMap.entrySet())
//				.flatMap(Map.Entry::getValue)
//				.replay()
//				.autoConnect(0);
//
//		// sleep until we actually start listening for new peers.
//		//TODO: add a notification in PeersListener class to notify when we actually started listening.
//		Thread.sleep(1000);
//
//		Link meToFakePeerLink = this.recordedMeToFakePeersLinks$.take(1)
//				.single()
//				.block();
//
//		BiFunction<Link, BlockOfPiece, RequestMessage> buildRequestMessage = (link, blockOfPiece) -> {
//			int pieceIndex = blockOfPiece.getPieceIndex() >= 0 ?
//					blockOfPiece.getPieceIndex() :
//					torrentInfo.getPieces().size() + blockOfPiece.getPieceIndex();
//
////			int requestBlockSize = blockOfPiece.getMessageLength() != null ?
////					blockOfPiece.getMessageLength() :
////					torrentInfo.getPieceLength(pieceIndex) - blockOfPiece.getOffset();
//
//			return new RequestMessage(link.getMe(), link.getPeer(), pieceIndex, blockOfPiece.getOffset(), requestBlockSize);
//		};
//
//		// download the blocks.
//		BlockDownloader blockDownloader = torrentDownloader
//				.getBittorrentAlgorithm()
//				.getDownloadAlgorithm()
//				.getBlockDownloader();
//		int concurrentRequestsToSend = peerRequestBlockList.size();
//		this.actualSavedBlocks$ = Flux.fromIterable(peerRequestBlockList)
//				.map(blockOfPiece -> buildRequestMessage.apply(meToFakePeerLink, blockOfPiece))
//				.flatMap(requestMessage ->
//						blockDownloader.downloadBlock(meToFakePeerLink, requestMessage)
//								.onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty()), concurrentRequestsToSend);
    }

    @Then("^application receive the following blocks from him - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingBlocksFromHimForTorrent(String torrentFileName,
                                                                      List<BlockOfPiece> expectedBlockFromFakePeerList) throws Throwable {
        // TODO: un comment this.
//		TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
//
//		Function<BlockOfPiece, BlockOfPiece> fixBlockOfPiece = blockOfPiece -> {
//			int pieceIndex = blockOfPiece.getPieceIndex() >= 0 ?
//					blockOfPiece.getPieceIndex() :
//					torrentInfo.getPieces().size() + blockOfPiece.getPieceIndex();
//
//			int requestBlockSize = blockOfPiece.getMessageLength() != null ?
//					blockOfPiece.getMessageLength() :
//					torrentInfo.getPieceLength(pieceIndex) - blockOfPiece.getOffset();
//
//			return new BlockOfPiece(pieceIndex, blockOfPiece.getOffset(), requestBlockSize);
//		};
//
//		List<BlockOfPiece> fixedExpectedBlockFromFakePeerList = expectedBlockFromFakePeerList.stream()
//				.map(fixBlockOfPiece)
//				.collect(Collectors.toList());
//
//		List<BlockOfPiece> actualDownloadedBlocks = this.actualSavedBlocks$.take(expectedBlockFromFakePeerList.size())
//				.map(PieceEvent::getReceivedPiece)
//				.map(pieceEvent -> new BlockOfPiece(pieceEvent.getIndex(),
//						pieceEvent.getBegin(), pieceEvent.getAllocatedBlock().length))
//				.collectList()
//				.block();
//
//		Assert.assertTrue(fixedExpectedBlockFromFakePeerList.stream()
//				.allMatch(actualDownloadedBlocks::contains));
//
//		Assert.assertTrue(actualDownloadedBlocks.stream()
//				.allMatch(fixedExpectedBlockFromFakePeerList::contains));
    }

    @Then("^application doesn't receive the following blocks from him - for torrent: \"([^\"]*)\":$")
    public void applicationDoesnTReceiveTheFollowingBlocksFromHimForTorrent(String torrentFileName,
                                                                            List<BlockOfPiece> notExpectedBlockFromFakePeerList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        StepVerifier.create(this.actualSavedBlocks$)
                .verifyComplete();
    }

    @Then("^fake-peer on port \"([^\"]*)\" choke me: \"([^\"]*)\" - for torrent: \"([^\"]*)\"$")
    public void fakePeerOnPortChokeMeForTorrent(Integer fakePeerPort, boolean isChoking, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        this.recordedFakePeersToMeLinks$.filter(remoteFakePeer -> remoteFakePeer.getMe().getPeerPort() == fakePeerPort)
                .flatMap(remoteFakePeer -> (isChoking ?
                        remoteFakePeer.sendMessages().sendChokeMessage() :
                        remoteFakePeer.sendMessages().sendUnchokeMessage())
                        .map(sendPeerMessages -> remoteFakePeer));


    }

    private Flux<Integer> availablePieces$;

    @When("^application request available pieces - for torrent: \"([^\"]*)\"$")
    public void applicationRequestAvailablePiecesForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        this.availablePieces$ = torrentDownloader.getBittorrentAlgorithm()
                .getDownloadAlgorithm()
                .getPeersToPiecesMapper()
                .getAvailablePiecesFlux();
    }

    @Then("^application receive the following available pieces - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingAvailablePiecesForTorrent(String torrentFileName,
                                                                        List<Integer> expectedAvailablePiecesList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .get();

        List<Integer> actualAvailablePiecesList = this.availablePieces$.collectList()
                .block(Duration.ofMillis(500));

        Assert.assertArrayEquals(expectedAvailablePiecesList.toArray(), actualAvailablePiecesList.toArray());
    }

    @When("^fake-peer on port \"([^\"]*)\" notify on more completed pieces using \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void fakePeerOnPortNotifyOnMoreCompletedPiecesUsingForTorrent(int fakePeerPort,
                                                                         String peerMessageType,
                                                                         String torrentFileName,
                                                                         List<Integer> fakePeerNotifyOnCompletedPieceList) throws Throwable {

    }

    @Then("^application receive the following extra available pieces - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingExtraAvailablePiecesForTorrent(String torrentFileName,
                                                                             List<Integer> expectedAvailablePiecesList) throws Throwable {

    }

    @When("^application request available peers for piece: \"([^\"]*)\" - for torrent: \"([^\"]*)\"$")
    public void applicationRequestAvailablePeersForPieceForTorrent(int pieceIndex, String torrentFileName) throws Throwable {

    }

    @Then("^application receive the following available fake-peers for piece: \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingAvailableFakePeersForPieceForTorrent(int pieceIndex,
                                                                                   String torrentFileName,
                                                                                   List<Integer> expectedAvailableFakePeerPortList) throws Throwable {
    }

    @Then("^application receive the following extra available fake-peers for piece: \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingExtraAvailablePeersForPieceForTorrent(int pieceIndex,
                                                                                    String torrentFileName,
                                                                                    List<Integer> expectedAvailableFakePeerPortList) throws Throwable {

    }

    @Then("^application receive none extra available pieces - for torrent: \"([^\"]*)\"$")
    public void applicationReceiveNoneExtraAvailablePiecesForTorrent(String torrentFileName) throws Throwable {

    }

    @Then("^application receive none available fake-peers for piece: \"([^\"]*)\" - for torrent: \"([^\"]*)\"$")
    public void applicationReceiveNoneAvailableFakePeersForPieceForTorrent(int pieceIndex, String torrentFileName) throws Throwable {

    }

    @Then("^application receive the none available pieces - for torrent: \"([^\"]*)\"$")
    public void applicationReceiveTheNoneAvailablePiecesForTorrent(String torrentFileName) throws Throwable {

    }

    @Then("^application download the following pieces - concurrent piece's downloads: \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadTheFollowingPiecesConcurrentPieceSDownloadsForTorrent(int concurrentPieceDownloads,
                                                                                         String torrentFileName,
                                                                                         List<Integer> piecesToDownloadList) throws Throwable {

    }

    @Then("^application downloaded the following pieces - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadedTheFollowingPiecesForTorrent(String torrentFileName,
                                                                  List<Integer> piecesDownloadedList) throws Throwable {

    }

    @Then("^application couldn't downloaded the following pieces - for torrent: \"([^\"]*)\":$")
    public void applicationCloudnTDownloadedTheFollowingPiecesForTorrent(String torrentFileName,
                                                                         List<Integer> piecesNotDownloadedList) throws Throwable {

    }

    // TODO: this test does not have good design because it collects all the pieceMessages to multiple sets so we can't
    // free the allocatedBlocks objects. We may run in a situation when each block is 1gb so we will get out of memory exception.
    @When("^application save the all the pieces of torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationSaveTheAllThePiecesOfTorrent(String torrentFileName, String downloadLocation) throws Throwable {
//        Utils.removeEverythingRelatedToLastTest();
//
//        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
//
//        StatusChanger statusChanger = new StatusChanger(new Status(
//                false,
//                false,
//                false,
//                false,
//                false, false,
//                false,
//                false,
//                false,
//                false,
//                false));
//
//        Thread.sleep(50);// wait until torrentStatusController is initialized.
//
//        ConnectableFlux<PieceMessage> allPieceMessages$ = Flux.range(0, torrentInfo.getPieces().size())
//                .map(pieceIndex -> new BlockOfPiece(pieceIndex, 0, null))
//                .map(blockOfPiece -> Utils.createRandomPieceMessages(torrentInfo, blockOfPiece, 1_000_000_000))
//                .flatMap(Flux::fromIterable)
//                .publish();
//
//        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;
//        FileSystemLinkImpl fileSystemLinkImplTorrent = ActiveTorrents.getInstance()
//                .createActiveTorrentMono(torrentInfo, fullDownloadPath, statusChanger, allPieceMessages$)
//                .block();
//
//        Mono<Integer> piecesAmount$ = allPieceMessages$.collectList()
//                .map(List::size)
//                .flux()
//                .replay()
//                .autoConnect(0)
//                .take(1)
//                .single();
//
//        Flux<Integer> allSavedPiecesRecorded$ = fileSystemLinkImplTorrent.savedPieceFlux()
//                .replay()
//                .autoConnect(0);
//
//        allPieceMessages$.connect();
//
//        StepVerifier.create(allSavedPiecesRecorded$)
//                .expectNextCount(torrentInfo.getPieces().size())
//                .verifyComplete();
//
//        Flux<PieceEvent> savedBlocks$ = fileSystemLinkImplTorrent.savedBlockFlux()
//                .replay()
//                .autoConnect(0);
//
//        StepVerifier.create(savedBlocks$)
//                .expectNextCount(piecesAmount$.block())
//                .verifyComplete();
//
//        Assert.assertEquals(fileSystemLinkImplTorrent.minMissingPieceIndex(), -1);
    }

    @And("^the saved-pieces-flux send complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedPiecesFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();
    }

    @And("^the saved-blocks-flux send  complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedBlocksFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();
    }

    @Then("^torrent-status change: \"([^\"]*)\" and notify only about the changes - for torrent \"([^\"]*)\":$")
    public void torrentStatusChangeAndNotifyOnlyAboutTheChangesForTorrent(StatusType statusTypeChanging, String torrentFileName) throws Throwable {

    }

    private BlocksAllocator blocksAllocator;

    @Given("^allocator for \"([^\"]*)\" blocks with \"([^\"]*)\" bytes each$")
    public void allocatorForBlocksWithBytesEach(int amountOfBlocksToAllocate, int blockLength) throws Throwable {
        this.blocksAllocator = new BlocksAllocatorImpl(amountOfBlocksToAllocate, blockLength);
    }

    private Set<AllocatedBlock> actualAllocations;

    @When("^the application allocate \"([^\"]*)\" blocks from \"([^\"]*)\" threads:$")
    public void theApplicationAllocateBlocksFromThreads(int amountOfAllocations, int threadsAmount) throws Throwable {
        Flux<Integer> frees$ = this.blocksAllocator.frees$()
                .replay()
                .autoConnect(0);

        this.actualAllocations = Flux.range(0, amountOfAllocations)
                .flatMap(blockIndex -> this.blocksAllocator.allocate(0, 0), threadsAmount)
                .doOnNext(allocatedBlock ->
                        Assert.assertFalse(this.blocksAllocator.getFreeBlocksStatus().get(allocatedBlock.getBlockIndex())))
                .collect(Collectors.toSet())
                .block();

        int actualFreesAmount = this.blocksAllocator.getAmountOfBlocks() - amountOfAllocations;
        frees$.take(actualFreesAmount)
                .doOnNext(freeIndex -> Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(freeIndex)))
                .collect(Collectors.toSet())
                .block();
    }

    @When("^the application allocate the following blocks from \"([^\"]*)\" threads:$")
    public void theApplicationAllocateTheFollowingBlocksFromThreads(int threadsAmount, List<Integer> expectedAllocationsList) throws Throwable {
        Flux<Integer> allocations$ = this.blocksAllocator.allocated$()
                .replay()
                .autoConnect(0);

        this.actualAllocations = Flux.range(0, expectedAllocationsList.size())
                .flatMap(blockIndex -> this.blocksAllocator.allocate(0, 0), threadsAmount)
                .doOnNext(allocatedBlock -> Assert.assertFalse(this.blocksAllocator.getFreeBlocksStatus().get(allocatedBlock.getBlockIndex())))
                .collect(Collectors.toSet())
                .block();

        Set<Integer> actualAllocationsFromNotifier = allocations$.take(expectedAllocationsList.size())
                .collect(Collectors.toSet())
                .block();

        Assert.assertEquals(expectedAllocationsList.stream()
                        .collect(Collectors.toSet()),
                actualAllocations.stream()
                        .map(AllocatedBlock::getBlockIndex)
                        .collect(Collectors.toSet()));
        Assert.assertEquals(expectedAllocationsList.stream().collect(Collectors.toSet()), actualAllocationsFromNotifier);
    }

    @Then("^the allocator have the following free blocks:$")
    public void theAllocatorHaveTheFollowingFreeBlocks(List<Integer> freeBlocksList) throws Throwable {
        for (int i = 0; i < this.blocksAllocator.getAmountOfBlocks(); i++)
            if (freeBlocksList.contains(i))
                Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(i));
            else
                Assert.assertFalse(this.blocksAllocator.getFreeBlocksStatus().get(i));
    }

    @Then("^the allocator have the following used blocks:$")
    public void theAllocatorHaveTheFollowingUsedBlocks(List<Integer> usedBlocksList) throws Throwable {
        for (int i = 0; i < this.blocksAllocator.getAmountOfBlocks(); i++)
            if (usedBlocksList.contains(i))
                Assert.assertFalse(this.blocksAllocator.getFreeBlocksStatus().get(i));
            else
                Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(i));
    }

    @Then("^the allocator have the following free blocks - none$")
    public void theAllocatorHaveTheFollowingFreeBlocksNone() throws Throwable {
        for (int i = 0; i < this.blocksAllocator.getAmountOfBlocks(); i++)
            Assert.assertFalse(this.blocksAllocator.getFreeBlocksStatus().get(i));
    }

    @When("^the application free the following blocks:$")
    public void theApplicationFreeTheFollowingBlocks(List<Integer> allocationToFreeList) throws Throwable {
        Flux<Integer> frees$ = this.blocksAllocator.frees$()
                .replay()
                .autoConnect(0);

        this.actualAllocations.stream()
                .filter(allocatedBlock -> allocationToFreeList.contains(allocatedBlock.getBlockIndex()))
                .peek(allocatedBlock -> this.blocksAllocator.free(allocatedBlock))
                .map(AllocatedBlock::getBlockIndex)
                .forEach(freedIndex -> Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(freedIndex)));

        frees$.take(allocationToFreeList.size())
                .doOnNext(freeIndex -> Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(freeIndex)))
                .collect(Collectors.toSet())
                .block();
    }

    @Then("^the allocator have the following used blocks - none$")
    public void theAllocatorHaveTheFollowingUsedBlocksNone() throws Throwable {
        for (int i = 0; i < this.blocksAllocator.getAmountOfBlocks(); i++)
            Assert.assertTrue(this.blocksAllocator.getFreeBlocksStatus().get(i));
    }
}

