package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.App;
import main.TorrentInfo;
import main.algorithms.*;
import main.algorithms.impls.v1.download.BlockDownloaderImpl;
import main.algorithms.impls.v1.download.PeersToPiecesMapperImpl;
import main.algorithms.impls.v1.download.PieceDownloaderImpl;
import main.algorithms.impls.v1.download.PiecesDownloaderImpl;
import main.algorithms.impls.v1.upload.UploadAlgorithmImpl;
import main.downloader.*;
import main.file.system.FileSystemLink;
import main.file.system.FileSystemLinkImpl;
import main.file.system.allocator.AllocatedBlock;
import main.file.system.allocator.AllocatorReducer;
import main.file.system.allocator.AllocatorState;
import main.file.system.allocator.AllocatorStore;
import main.listener.Listener;
import main.listener.ListenerAction;
import main.listener.reducers.ListenerReducer;
import main.listener.state.tree.ListenerState;
import main.peer.*;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.reducers.TorrentStatusReducer;
import main.torrent.status.side.effects.TorrentStatesSideEffects;
import main.torrent.status.state.tree.TorrentStatusState;
import main.tracker.Tracker;
import main.tracker.TrackerConnection;
import main.tracker.TrackerExceptions;
import main.tracker.TrackerProvider;
import main.tracker.response.TrackerResponse;
import org.junit.Assert;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import redux.store.Store;

import java.io.File;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class MyStepdefs {
    private static Logger logger = LoggerFactory.getLogger(MyStepdefs.class);
    private static Scheduler testParallelScheduler = Schedulers.newParallel("TEST-HELPER-PARALLEL", 1);

    public static AllocatorStore globalFakePeerAllocator = new AllocatorStore(new Store<>(new AllocatorReducer(),
            AllocatorReducer.defaultAllocatorState, "Global-Fake-Peer-Allocator"));

    static {
        Hooks.onErrorDropped(throwable -> {
        });

        // active debug mode in reactor
        Hooks.onOperatorDebug();

        Utils.removeEverythingRelatedToLastTest();

        MyStepdefs.globalFakePeerAllocator.updateAllocations(10, 2_500_000)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
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
                .publish()
                .autoConnect(1)
                .flatMap(trackerConnection ->
                        Flux.fromIterable(messages)
                                .filter(fakeMessage -> fakeMessage.getTrackerRequestType() != TrackerRequestType.Connect)
                                // given a tracker, communicateMono with him and get the signal containing the response.
                                .flatMap(messageWeNeedToSend -> {
                                    switch (messageWeNeedToSend.getTrackerRequestType()) {
                                        case Announce:
                                            return TorrentDownloaders.getListener()
                                                    .getListeningPort()
                                                    // If we get timeOut then it means that we are not listening so I will just fake a random port which will ignore incoming connections.
                                                    .timeout(Duration.ofSeconds(2), Mono.just(12345))
                                                    .flatMap(listeningPort -> trackerConnection.announceMono(this.torrentInfo.getTorrentInfoHash(), listeningPort));
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

    @Then("^application interested in all peers for torrent: \"([^\"]*)\"$")
    public void applicationInterestedInAllPeersForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
    }

    @When("^application create active-torrent for: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationCreateActiveTorrentFor(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();
        System.out.println("finished clean after last test...");
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // this will waitForMessage an activeTorrent object.
        String downloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setToDefaultIncomingPeerMessages()
                .setToDefaultEmitIncomingPeerMessages()
                .setToDefaultAllocatorStore()
                .setToDefaultTorrentStatusStore("Test-App-TorrentStatus-Store")
                .setToDefaultTorrentStatesSideEffects()
                .setToDefaultSearchPeers()
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultFileSystemLink(downloadPath)
                .build();
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        // Note: this status is useless because we don't use ActiveTorrents class so there is no need to test anything.
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
                    (torrentFile, path) -> new File(path))
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

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .map(TorrentDownloader::getTorrentStatusStore)
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        torrentStatusStore.latestState$()
                .as(state$ -> {
                    if (deleteTorrentFiles)
                        return state$.flatMap(__ -> torrentStatusStore.dispatch(TorrentStatusAction.REMOVE_FILES_IN_PROGRESS))
                                .flatMap(__ -> torrentStatusStore.notifyWhen(TorrentStatusAction.REMOVE_FILES_WIND_UP));
                    return state$;
                })
                .as(state$ -> {
                    if (deleteActiveTorrent)
                        return state$.flatMap(__ -> torrentStatusStore.dispatch(TorrentStatusAction.REMOVE_TORRENT_IN_PROGRESS))
                                .flatMap(__ -> torrentStatusStore.notifyWhen(TorrentStatusAction.REMOVE_TORRENT_WIND_UP));
                    return state$;
                })
                .doOnNext(__ -> System.out.println(1))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^application save the all the pieces of torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationSaveTheAllThePiecesOfTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore()
                .updateAllocations(10, 500_000)
                .block();

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        TorrentStatesSideEffects sideEffects = new TorrentStatesSideEffects(torrentInfo, torrentStatusStore);

        // release new next signal only when we finish working on the last one and only after we cleaned it's buffer.
        // ..........................................................................................................
        // the permits must be 1 because if not, I may write to the same piece
        // concurrently and piece counter will not be accurate.
        Semaphore semaphore = new Semaphore(1, true);

        ConnectableFlux<PieceMessage> allBlocksMessages$ =
                TorrentDownloaders.getAllocatorStore()
                        .latestState$()
                        .map(AllocatorState::getBlockLength)
                        .flatMapMany(allocatedBlockLength -> Flux.range(0, torrentInfo.getPieces().size())
                                .map(pieceIndex -> new BlockOfPiece(pieceIndex, 0, null))
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength))
                                .flatMap(blockOfPiece -> Utils.createRandomPieceMessages(TorrentDownloaders.getAllocatorStore(), torrentInfo, semaphore, blockOfPiece, allocatedBlockLength)))
                        .doOnNext(pieceMessage -> System.out.println("saving: " + pieceMessage.getIndex() + "," + pieceMessage.getBegin() + "," + pieceMessage.getAllocatedBlock().getLength()))
                        .publishOn(MyStepdefs.testParallelScheduler)
                        .publish();

        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setToDefaultIncomingPeerMessages()
                .setToDefaultEmitIncomingPeerMessages()
                .setToDefaultAllocatorStore()
                .setTorrentStatusStore(torrentStatusStore)
                .setTorrentStatesSideEffects(sideEffects)
                .setFileSystemLink(new FileSystemLinkImpl(torrentInfo, fullDownloadPath, TorrentDownloaders.getAllocatorStore(), torrentStatusStore, allBlocksMessages$, "App"))
                .build();
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);
        Mono<List<Integer>> piecesCompleted1 = torrentDownloader.getFileSystemLink().savedBlocks$()
                //.doOnNext(pieceEvent -> System.out.println("block complete:" + pieceEvent))
                .flatMap(pieceEvent -> {
                    AllocatedBlock allocatedBlock = pieceEvent.getReceivedPiece().getAllocatedBlock();
                    return TorrentDownloaders.getAllocatorStore()
                            .free(allocatedBlock)
                            .map(allocatorState -> pieceEvent);
                })
                .doOnNext(pieceEvent -> semaphore.release())
                .filter(pieceEvent -> pieceEvent.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
                .map(PieceEvent::getReceivedPiece)
                .map(PieceMessage::getIndex)
                .doOnNext(pieceIndex -> System.out.println("saved piece index: " + pieceIndex))
                .replay()
                .autoConnect(0)
                .collectList();

        Mono<List<Integer>> piecesCompleted2 = torrentDownloader.getFileSystemLink().savedPieces$()
                .replay()
                .autoConnect(0)
                .collectList();

        allBlocksMessages$.connect();

        List<Integer> expected = piecesCompleted1.block();
        List<Integer> actual = piecesCompleted2.block();
        Utils.assertListEqualNotByOrder(expected, actual, Integer::equals);
    }

    private Flux<Integer> actualCompletedSavedPiecesReadByFS$ = null;

    @When("^application save random blocks for torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore()
                .updateAllocations(4, torrentInfo.getPieceLength(0))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        // I only need this object because the constructor of FileSystemLink listen to signals (which I never signal).
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        // I need this object for the cleanup after this test so I can wait until all the files were actually removed.
        TorrentStatesSideEffects sideEffects = new TorrentStatesSideEffects(torrentInfo, torrentStatusStore);

        // release new next signal only when we finish working on the last one and only after we cleaned it's buffer.
        Semaphore semaphore = new Semaphore(1, true);

        Flux<PieceMessage> generatedWrittenPieceMessages$ = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(blockList)
                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength))
                        .doOnNext(blockOfPiece -> System.out.println("start saving: " + blockOfPiece))
                        .flatMap(blockOfPiece -> Utils.createRandomPieceMessages(TorrentDownloaders.getAllocatorStore(), torrentInfo, semaphore, blockOfPiece, allocatedBlockLength)))
                .publish()
                .autoConnect(2);

        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setToDefaultIncomingPeerMessages()
                .setToDefaultEmitIncomingPeerMessages()
                .setToDefaultAllocatorStore()
                .setTorrentStatusStore(torrentStatusStore)
                .setTorrentStatesSideEffects(sideEffects)
                .setToDefaultSearchPeers()
                .setToDefaultPeersCommunicatorFlux()
                .setFileSystemLink(new FileSystemLinkImpl(torrentInfo, fullDownloadPath, TorrentDownloaders.getAllocatorStore(), torrentStatusStore, generatedWrittenPieceMessages$, "App"))
                .build();
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);


        // this.actualCompletedSavedPiecesReadByFS$ will be used in later step.
        this.actualCompletedSavedPiecesReadByFS$ = torrentDownloader.getFileSystemLink()
                .savedPieces$()
                .replay()
                .autoConnect(0);

        Flux<PieceMessage> customPieces$ = torrentDownloader.getFileSystemLink()
                .savedBlocks$()
                .map(PieceEvent::getReceivedPiece);

        Flux.zip(customPieces$, generatedWrittenPieceMessages$)
                .doOnNext(pieces -> Assert.assertEquals("the FS notifier notified about other block which" +
                        " he saved than the block we expected to save.", pieces.getT1(), pieces.getT2()))
                .map(Tuple2::getT1)
                .doOnNext(pieceMessage -> System.out.println("saved from: " + pieceMessage.getBegin() +
                        ", length: " + pieceMessage.getAllocatedBlock().getLength()))
                .concatMap(pieceMessage -> TorrentDownloaders.getAllocatorStore()
                        .createRequestMessage(null, null, pieceMessage.getIndex(),
                                pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getLength(),
                                torrentInfo.getPieceLength(pieceMessage.getIndex()))
                        .flatMap(requestMessage -> Utils.readFromFile(TorrentDownloaders.getAllocatorStore(), torrentInfo, fullDownloadPath, requestMessage)
                                .doOnNext(actualPieceMessage -> System.out.println("assert actualPieceFromFS: " +
                                        "saved from: " + actualPieceMessage.getBegin() +
                                        ", length: " + actualPieceMessage.getAllocatedBlock().getLength() +
                                        " <----> generatedPieceMessage: " +
                                        "saved from: " + pieceMessage.getBegin() +
                                        ", length: " + pieceMessage.getAllocatedBlock().getLength()))
                                .doOnNext(actualPieceMessage -> Assert.assertEquals("the pieces we read from filesystem " +
                                                "are not equal to the pieces we tried to save to the filesystem.",
                                        pieceMessage.getAllocatedBlock(), actualPieceMessage.getAllocatedBlock()))
                                // free the write and read blocks.
                                .flatMap(actualPieceMessage -> TorrentDownloaders.getAllocatorStore().free(actualPieceMessage.getAllocatedBlock()))
                                .flatMap(__ -> TorrentDownloaders.getAllocatorStore().free(pieceMessage.getAllocatedBlock()))))
                // tell upstream that we freed the buffer and he can give us one more signal (if he have any left)
                .doOnNext(__ -> semaphore.release())
                .collectList()
                .doOnNext(__ -> logger.debug("completed saving all the blocks for this test."))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .doOnNext(allocatorState -> {
                    IntStream.range(0, allocatorState.getAmountOfBlocks())
                            .forEach(i -> Assert.assertTrue("i: " + i + " - global app allocator: " + allocatorState.toString(),
                                    allocatorState.getFreeBlocksStatus().get(i)));
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        MyStepdefs.globalFakePeerAllocator
                .latestState$()
                .doOnNext(allocatorState -> {
                    IntStream.range(0, allocatorState.getAmountOfBlocks())
                            .forEach(i -> Assert.assertTrue("i: " + i + " - global fake-peer allocator: " + allocatorState.toString(),
                                    allocatorState.getFreeBlocksStatus().get(i)));
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^the only completed pieces are - for torrent: \"([^\"]*)\":$")
    public void completedPiecesAreForTorrent(String torrentFileName,
                                             List<Integer> completedPiecesIndexList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        Set<Integer> fixedCompletedPiecesIndexList = completedPiecesIndexList.stream()
                .map(pieceIndex -> pieceIndex >= 0 ?
                        pieceIndex :
                        torrentInfo.getPieces().size() + pieceIndex)
                .collect(Collectors.toSet());

        Set<Integer> actualCompletedPiecesByFSNotifierSet = this.actualCompletedSavedPiecesReadByFS$.collect(Collectors.toSet())
                .block();

        Assert.assertEquals("the expected and actual completed pieces indexes are not equal.",
                fixedCompletedPiecesIndexList, actualCompletedPiecesByFSNotifierSet);

        FileSystemLink fileSystemLinkImplTorrent = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getFileSystemLink();

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
    public void applicationConnectToAllPeersAndAssertThatWeConnectedToThemForTorrent(String torrentFileName) {
//        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
//
//        Utils.removeEverythingRelatedToLastTest();
//
//        // we won't download anything but we still need to specify a path to download to.
//        String DEFAULT_DOWNLOAD_LOCATION = System.getProperty("user.dir") + File.separator + "torrents-test/";
//
//        TorrentDownloaderBuilder.buildDefault(torrentInfo, DEFAULT_DOWNLOAD_LOCATION)
//                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
//                .doOnNext(torrentDownloader -> {
//                    // consume new peers and new responses from 1.5 seconds.
//                    // filter distinct peers from the responses, and assert
//                    // that both the list of peers are equal.
//
//                    Flux<Peer> connectedPeersFlux = torrentDownloader.getPeersCommunicatorFlux()
//                            .map(Link::getPeer)
//                            .timeout(Duration.ofMillis(1500))
//                            .buffer(Duration.ofMillis(1500))
//                            .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
//                            .take(3)
//                            .flatMap(Flux::fromIterable)
//                            .sort();
//
//                    Flux<Peer> peersFromResponsesMono = torrentDownloader.getPeersCommunicatorFlux()
//                            .map(Link::receivePeerMessages)
//                            .flatMap(IncomingPeerMessagesNotifier::getPeerMessageResponseFlux)
//                            .map(PeerMessage::getFrom)
//                            .distinct()
//                            .timeout(Duration.ofMillis(1500))
//                            .buffer(Duration.ofMillis(1500))
//                            .onErrorResume(TimeoutException.class, throwable -> Flux.empty())
//                            .take(2)
//                            .flatMap(Flux::fromIterable)
//                            .sort()
//                            // I'm going to get this peers again AFTER:
//                            // torrentDownloader.getTorrentStatusStore().start();
//                            .replay()
//                            .autoConnect();
//
//                    // for recording all the peers without blocking the main thread.
//                    peersFromResponsesMono.subscribe();
//
//
//                    torrentDownloader.getTorrentStatusStore().dispatch(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS).block();
//
//                    List<Peer> connectedPeers = connectedPeersFlux.collectList().block();
//                    List<Peer> peersFromResponses = peersFromResponsesMono.collectList().block();
//
//                    peersFromResponses.stream()
//                            .filter(connectedPeers::contains)
//                            .findFirst()
//                            .ifPresent(peer -> Assert.fail("We received from the following peer" +
//                                    " messages but he doesn't exist in the connected peers flux: " + peer));
//                })
//                .as(StepVerifier::create)
//                .expectNextCount(1)
//                .verifyComplete();
//
//        Utils.removeEverythingRelatedToLastTest();
    }

    @Given("^initial torrent-status for torrent: \"([^\"]*)\" in \"([^\"]*)\" is - no side effects:$")
    public void activeTorrentForInWithTheFollowingStatus(String torrentFileName, String downloadLocation,
                                                         List<TorrentStatusAction> torrentStatusActions) throws Throwable {

        Utils.removeEverythingRelatedToLastTest();
        System.out.println("finished clean the last test data");

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // clean from the last test.
        TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        TorrentStatusState torrentStatusState = Utils.getTorrentStatusState(torrentInfo, TorrentStatusAction.INITIALIZE, torrentStatusActions);
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(), torrentStatusState, "Test-App-TorrentStatus-Store");

        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setToDefaultIncomingPeerMessages()
                .setToDefaultEmitIncomingPeerMessages()
                .setToDefaultAllocatorStore()
                .setTorrentStatusStore(torrentStatusStore)
                .build();
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);
    }

    private TorrentStatusState actualLastStatus = null;

    @When("^torrent-status for torrent \"([^\"]*)\" is trying to change to - no side effects:$")
    public void torrentStatusForIsTryingToChangeTo(String torrentFileName,
                                                   List<TorrentStatusAction> changeTorrentStatusActionList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        Store<TorrentStatusState, TorrentStatusAction> store = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore();

        this.actualLastStatus = Flux.fromIterable(changeTorrentStatusActionList)
                .flatMap(store::dispatch, changeTorrentStatusActionList.size(), changeTorrentStatusActionList.size())
                .blockLast();
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be with action: \"([^\"]*)\" - no side effects:$")
    public void torrentStatusForWillBeWithoutSideEffects(String torrentFileName, TorrentStatusAction lastTorrentStatusAction,
                                                         List<TorrentStatusAction> expectedTorrentStatusActionList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore();

        TorrentStatusState expectedState = Utils.getTorrentStatusState(torrentInfo, lastTorrentStatusAction, expectedTorrentStatusActionList);

        // test with the actual last status we received in the last time we tried to change the status
        if (this.actualLastStatus != null) {
            Assert.assertEquals(expectedState.getAction(), this.actualLastStatus.getAction());
            Assert.assertEquals(expectedState.getDownloadState(), this.actualLastStatus.getDownloadState());
            Assert.assertEquals(expectedState.getTorrentFileSystemState(), this.actualLastStatus.getTorrentFileSystemState());
            Assert.assertEquals(expectedState.getSearchPeersState(), this.actualLastStatus.getSearchPeersState());
            this.actualLastStatus = null;
        }

        torrentStatusStore.latestState$()
                .doOnNext(torrentStatusState -> {
                    Assert.assertEquals(expectedState.getAction(), torrentStatusState.getAction());
                    Assert.assertEquals(expectedState.getDownloadState(), torrentStatusState.getDownloadState());
                    Assert.assertEquals(expectedState.getTorrentFileSystemState(), torrentStatusState.getTorrentFileSystemState());
                    Assert.assertEquals(expectedState.getSearchPeersState(), torrentStatusState.getSearchPeersState());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be with action: \"([^\"]*)\":$")
    public void torrentStatusForWillBe(String torrentFileName, TorrentStatusAction lastTorrentStatusAction,
                                       List<TorrentStatusAction> expectedTorrentStatusActionList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentStatusState expectedState = Utils.getTorrentStatusState(torrentInfo, lastTorrentStatusAction, expectedTorrentStatusActionList);

        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore()
                .latestState$()
                .doOnNext(torrentStatusState -> {
                    Assert.assertEquals(expectedState.getAction(), torrentStatusState.getAction());
                    Assert.assertEquals(expectedState.getDownloadState(), torrentStatusState.getDownloadState());
                    Assert.assertEquals(expectedState.getTorrentFileSystemState(), torrentStatusState.getTorrentFileSystemState());
                    Assert.assertEquals(expectedState.getSearchPeersState(), torrentStatusState.getSearchPeersState());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    private Mono<Link> fakePeerToAppLink$;
    private Mono<List<SendMessagesNotifications>> requestsFromFakePeerToMeList$;
    private Flux<AllocatorState> freePiecesFromApp$;

    @Then("^random-fake-peer connect to me for torrent: \"([^\"]*)\" in \"([^\"]*)\" and he request:$")
    public void randomFakePeerConnectToMeForTorrentInAndHeRequest(String torrentFileName, String downloadLocation,
                                                                  List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // The last step created FileSystemLinkImpl object which listener to custom
        // peerResponsesFlux. So I can't expect it to react to the original peerResponsesFlux.
        // Also the last test created torrentStatusController object.
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        // this flux is empty because if not, the application will get the peers from
        // them and then it will connect to all those peers and then those peers will
        // sendMessage me incoming messages and I don't want any incoming messages but the
        // messages from my fake-peer.
        Flux<TrackerConnection> trackerConnectionFlux = Flux.empty();

        TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$ = EmitterProcessor.create();
        FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages = incomingPeerMessages$.sink();
        // represent this application TorrentDownloader. (not the fake-peer TorrentDownloader).
        TrackerProvider trackerProvider = Mockito.mock(TrackerProvider.class);
        Mockito.when(trackerProvider.connectToTrackersFlux()).thenReturn(trackerConnectionFlux);
        TorrentDownloader torrentDownloader$ = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setIncomingPeerMessages(incomingPeerMessages$)
                .setEmitIncomingPeerMessages(emitIncomingPeerMessages)
                .setToDefaultAllocatorStore()
                .setTorrentStatusStore(torrentDownloader.getTorrentStatusStore())
                .setToDefaultTorrentStatesSideEffects()
                .setSearchPeers(new SearchPeers(torrentInfo, torrentDownloader.getTorrentStatusStore(), "App", trackerProvider,
                        new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo, "App", emitIncomingPeerMessages)))
                .setToDefaultPeersCommunicatorFlux()
                .setFileSystemLink(torrentDownloader.getFileSystemLink())
                .setToDefaultBittorrentAlgorithm()
                .build();
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader$);

        EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessagesFakePeer$ = EmitterProcessor.create();
        FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessagesFakePeer = incomingPeerMessagesFakePeer$.sink();
        IncomingPeerMessagesNotifierImpl incomingPeerMessagesNotifier = new IncomingPeerMessagesNotifierImpl(incomingPeerMessagesFakePeer$);
        this.freePiecesFromApp$ = incomingPeerMessagesNotifier.getPieceMessageResponseFlux()
                .flatMap(pieceMessage -> MyStepdefs.globalFakePeerAllocator.free(pieceMessage.getAllocatedBlock()))
                .replay()
                .autoConnect(0);

        // start listen -> make fake peer connect to me -> send fake messages from fake-peer to me.
        this.fakePeerToAppLink$ = TorrentDownloaders.getListenStore()
                .dispatch(ListenerAction.START_LISTENING_IN_PROGRESS)
                .flatMap(__ -> torrentDownloader.getTorrentStatusStore().dispatch(TorrentStatusAction.START_UPLOAD_IN_PROGRESS))
                .flatMap(__ -> TorrentDownloaders.getListenStore().notifyWhen(ListenerAction.START_LISTENING_WIND_UP))
                .flatMap(__ -> torrentDownloader.getTorrentStatusStore().notifyWhen(TorrentStatusAction.RESUME_UPLOAD_WIND_UP))
                .map(__ -> new PeersProvider(MyStepdefs.globalFakePeerAllocator, torrentInfo, "fake-peer", emitIncomingPeerMessagesFakePeer))
                .doOnNext(__ -> logger.debug("fake-peer trying to connect to the app."))
                .flatMap(peersProvider ->
                        TorrentDownloaders.getListener()
                                .getListeningPort()
                                .timeout(Duration.ofSeconds(2))
                                // the fake-peer will connect to me.
                                .map(listeningPort -> new Peer("localhost", listeningPort))
                                .flatMap(me -> peersProvider.connectToPeerMono(me)))
                .doOnNext(__ -> logger.debug("fake-peer connected to the app and start sending requests to the app."))
                .cache();
        this.requestsFromFakePeerToMeList$ = fakePeerToAppLink$
                .map(Link::sendMessages)
                .flatMapMany(sendMessagesObject -> sendMessagesObject.sendInterestedMessage()
                        .flatMapMany(__ -> MyStepdefs.globalFakePeerAllocator.latestState$()
                                .map(AllocatorState::getBlockLength)
                                // sendMessage all requests from fake peer to me.
                                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(peerRequestBlockList)
                                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength))))
                        .concatMap(blockOfPiece -> sendMessagesObject.sendRequestMessage(blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(), blockOfPiece.getLength())
                                .doOnNext(__ -> logger.debug("fake peer sent request for block: " + blockOfPiece))))
                .collectList()
                .doOnNext(requestList -> Assert.assertEquals("We sent less requests then expected.",
                        peerRequestBlockList.size(), requestList.size()))
                .doOnNext(__ -> logger.debug("fake-peer sent all the requests to the app."))
                .cache();
    }

    @Then("^we assert that for torrent: \"([^\"]*)\", we gave the following pieces to the random-fake-peer:$")
    public void weAssertThatForTorrentWeGaveTheFollowingPiecesToTheRandomFakePeer(String torrentFileName,
                                                                                  List<BlockOfPiece> expectedBlockFromMeList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        Mono<Link> meToFakePeerLink = torrentDownloader.getPeersCommunicatorFlux()
                .take(1)
                .single()
                .cache();

        // I must record this because when I subscribe to this.requestsFromFakePeerToMeList$,
        // fake-peer will send me request messages and I response to him **piece messages**
        // which I don't want to lose.
        logger.debug("app start listen for incoming requests from fake-peer.");
        Flux<PieceMessage> recordedPieceMessageFlux = meToFakePeerLink
                .map(Link::sendMessages)
                .flatMapMany(SendMessagesNotifications::sentPeerMessages$)
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class)
                .replay()
                .autoConnect(0);

        // I must subscribe to this separably because if the number of requests is zero then take(0) will complete the flux before susbcribing to it
        // and then the mono requestsFromFakePeerToMeList$ will never get subscribed.
        //...................................................................................................
        // send request massages from fake peer to me and get all the
        // piece messages from me to fake peer and collect them to list.
        logger.debug("fake-peer start sending requests to app.");
        StepVerifier.create(this.requestsFromFakePeerToMeList$)
                .expectNextCount(1)
                .verifyComplete();

        logger.debug("start asserting that the app actually sent back the pieces to each request.");

        Set<BlockOfPiece> actualBlockFromMeSet = recordedPieceMessageFlux
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getLength()))
                .take(expectedBlockFromMeList.size())
                .collect(Collectors.toSet())
                .block();
        logger.debug("the test collected all the pieces-messages the app actually repleyed to fake peer.");

        Set<BlockOfPiece> expectedBlockFromMeSet = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(expectedBlockFromMeList)
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .collect(Collectors.toSet())
                .block();

        // assert that both the list are equal.
        Assert.assertEquals(expectedBlockFromMeSet, actualBlockFromMeSet);

        logger.debug("test passes successfully. start cleaning all the resources by closing the connection with the fake-peer.");
        meToFakePeerLink.doOnNext(Link::dispose)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        this.freePiecesFromApp$.take(expectedBlockFromMeList.size())
                .as(StepVerifier::create)
                .expectNextCount(expectedBlockFromMeList.size())
                .verifyComplete();

        logger.debug("end cleaning all the resources of the test by removing everything I did here.");

        this.fakePeerToAppLink$.doOnNext(Link::dispose)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        Utils.removeEverythingRelatedToLastTest();
        logger.debug("ended cleaning all the resources of the test.");
    }

    private FluxSink<Link> emitMeToFakePeerLinks;
    private Flux<Link> meToFakePeerLink$;

    private FluxSink<PieceEvent> emitActualIncomingPieceMessages;
    private Flux<PieceEvent> actualIncomingPieceMessages$;

    private PeersToPiecesMapper peersToPiecesMapper;
    private List<AbstractMap.SimpleEntry<Integer, RemoteFakePeer>> fakePeersByPort;

    private Flux<GroupedFlux<Integer, Link>> linksByAvailableMissingPiece$;

    @Given("^torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void torrent(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        this.fakePeersByPort = new LinkedList<>();
        if (this.meToFakePeerLink$ != null)
            this.meToFakePeerLink$.doOnNext(Link::dispose)
                    .collectList()
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();

        UnicastProcessor<Link> meToFakePeerLinkProcessor = UnicastProcessor.create();
        this.emitMeToFakePeerLinks = meToFakePeerLinkProcessor.sink();
        this.meToFakePeerLink$ = meToFakePeerLinkProcessor.replay()
                .autoConnect(0);

        UnicastProcessor<PieceEvent> actualIncomingPieceMessages = UnicastProcessor.create();
        this.emitActualIncomingPieceMessages = actualIncomingPieceMessages.sink();
        this.actualIncomingPieceMessages$ = actualIncomingPieceMessages.replay()
                .autoConnect(0);

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore()
                .updateAllocations(10, torrentInfo.getPieceLength(0))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        Flux<TrackerConnection> trackers$ = Flux.empty();
        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessages$ = EmitterProcessor.create();
        FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages = incomingPeerMessages$.sink();
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        TrackerProvider trackerProvider = Mockito.mock(TrackerProvider.class);
        Mockito.when(trackerProvider.connectToTrackersFlux()).thenReturn(trackers$);

        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.builder(torrentInfo, "App")
                .setIncomingPeerMessages(incomingPeerMessages$)
                .setEmitIncomingPeerMessages(emitIncomingPeerMessages)
                .setToDefaultAllocatorStore()
                .setTorrentStatusStore(torrentStatusStore)
                .setToDefaultTorrentStatesSideEffects()
                .setSearchPeers(new SearchPeers(torrentInfo, torrentStatusStore, "App", trackerProvider,
                        new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo, "App", emitIncomingPeerMessages)))
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultFileSystemLink(fullDownloadPath)
                .build();
        this.peersToPiecesMapper = new PeersToPiecesMapperImpl(
                torrentDownloader.getTorrentInfo(),
                torrentDownloader.getIncomingPeerMessagesNotifier(),
                torrentDownloader.getPeersCommunicatorFlux(),
                torrentDownloader.getFileSystemLink().getUpdatedPiecesStatus());
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);

        this.linksByAvailableMissingPiece$ = this.peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                .replay()
                .autoConnect(0);

        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getPeersCommunicatorFlux()
                // send interested message to the fake-peer.
                .flatMap(link -> link.sendMessages().sendInterestedMessage().map(sendPeerMessages -> link))
                .doOnNext(link -> this.emitMeToFakePeerLinks.next(link))
                .replay()
                .autoConnect(0);
    }

    @Given("^link to \"([^\"]*)\" - fake-peer on port \"([^\"]*)\" with the following pieces - with delay: \"([^\"]*)\" milliseconds - for torrent: \"([^\"]*)\"$")
    public void linkToFakePeerWithTheFollowingPiecesForTorrent(FakePeerType fakePeerType,
                                                               int fakePeerPort,
                                                               int delayInMilliSeconds,
                                                               String torrentFileName,
                                                               List<Integer> fakePeerCompletedPieces) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // build a bitField message so I can send it to my app and also
        // the fake-peer will update his status that he have those pieces.
        BitSet bitSet = new BitSet(torrentInfo.getPieces().size());
        fakePeerCompletedPieces.stream()
                .map(completedPieceIndex -> completedPieceIndex >= 0 ?
                        completedPieceIndex :
                        torrentInfo.getPieces().size() + completedPieceIndex)
                .forEach(completedPieceIndex -> bitSet.set(completedPieceIndex));

        EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessagesFakePeer$ = EmitterProcessor.create();
        TorrentDownloaders.getListener()
                .getListeningPort()
                .map(listeningPort -> new Peer("localhost", listeningPort))
                .doOnNext(peer -> logger.info(fakePeerPort + " - received port from listener: " + peer))
                .delayElement(Duration.ofMillis(delayInMilliSeconds))
                .doOnNext(peer -> logger.info(fakePeerPort + " - waited: " + delayInMilliSeconds))
                .flatMap(me -> {
                    FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessagesFakePeer = incomingPeerMessagesFakePeer$.sink();
                    return new PeersProvider(MyStepdefs.globalFakePeerAllocator, torrentInfo, "Fake-peer", emitIncomingPeerMessagesFakePeer).connectToPeerMono(me);
                })
                .doOnNext(peer -> logger.info("fake peer: " + fakePeerPort + " connected to app -->> " + peer.getMe().getPeerPort()))
                .map(fakePeerToAppLink -> new RemoteFakePeer(MyStepdefs.globalFakePeerAllocator, fakePeerToAppLink, fakePeerType,
                        "Fake-peer-" + fakePeerPort + "-" + fakePeerType.toString(),
                        new IncomingPeerMessagesNotifierImpl(incomingPeerMessagesFakePeer$)))
                .doOnNext(remoteFakePeer -> logger.info("created fake-peer " + remoteFakePeer.getLink().getMe().getPeerPort() + "  object"))
                .doOnNext(remoteFakePeer -> {
                    synchronized (this.fakePeersByPort) {
                        this.fakePeersByPort.add(new AbstractMap.SimpleEntry<>(fakePeerPort, remoteFakePeer));
                    }
                })
                .doOnNext(remoteFakePeer -> logger.info(remoteFakePeer.getLink().getMe().getPeerPort() + " - added fake peer to the list of fake peers"))
                .flatMap(remoteFakePeer -> remoteFakePeer.getLink().sendMessages().sendBitFieldMessage(bitSet).map(sendPeerMessages -> remoteFakePeer)
                        .doOnNext(peer -> logger.info("fake-peer " + remoteFakePeer.getLink().getMe().getPeerPort() + " sent to app a bitfield message:" + bitSet)))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^application request the following blocks from all fake-peers - for torrent: \"([^\"]*)\":$")
    public void applicationRequestTheFollowingBlocksFromFakePeerOnPortForTorrent(String torrentFileName,
                                                                                 List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        final int concurrentRequestsToSend = peerRequestBlockList.size();

        BlockDownloaderImpl blockDownloader = new BlockDownloaderImpl(torrentInfo, torrentDownloader.getFileSystemLink(), torrentDownloader.getIdentifier());

        TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(peerRequestBlockList)
                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                // create the request message
                .flatMap(blockOfPiece -> this.meToFakePeerLink$
                                .flatMap(link -> TorrentDownloaders.getAllocatorStore().createRequestMessage(link.getMe(), link.getPeer(), blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(), blockOfPiece.getLength(), torrentInfo.getPieceLength(blockOfPiece.getPieceIndex()))
                                        .flatMap(requestMessage -> blockDownloader.downloadBlock(link, requestMessage)
                                                .doOnError(PeerExceptions.peerNotResponding, throwable -> logger.info("App failed to get the piece: " + requestMessage))
                                                .onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty())))
                                .take(1)
                        , concurrentRequestsToSend > 0 ? concurrentRequestsToSend : 1)
                .doOnNext(pieceEvent -> logger.info("App received from fake-peer the piece: " + pieceEvent))
                .doOnNext(pieceEvent -> this.emitActualIncomingPieceMessages.next(pieceEvent))
                .publish()
                .autoConnect(0);
    }

    @Then("^application receive the following blocks from all - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingBlocksFromHimForTorrent(String torrentFileName,
                                                                      List<BlockOfPiece> expectedBlockFromFakePeerList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // I'm implicitly assuming that the allocator of the fake-peer is big enough
        // to send me back exactly the blocks I want with the specified size I wanted.
        List<BlockOfPiece> expectedIncomingBlocks = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(expectedBlockFromFakePeerList)
                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .collectList()
                .block();

        this.actualIncomingPieceMessages$.take(expectedBlockFromFakePeerList.size())
                // the timeout is only for stopping a failed test in the CI servers.
                //.timeout(Duration.ofSeconds(5))
                .map(PieceEvent::getReceivedPiece)
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getLength()))
                .collectList()
                .doOnNext(actualIncomingBlocks -> Utils.assertListEqualNotByOrder(expectedIncomingBlocks, actualIncomingBlocks, BlockOfPiece::equals))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        // release reactor lib resources.
        this.emitMeToFakePeerLinks.complete();
        this.emitActualIncomingPieceMessages.complete();

        // close the connection of all the me-to-fake-peers
        this.meToFakePeerLink$.doOnNext(Link::dispose)
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application doesn't receive the following blocks from him - for torrent: \"([^\"]*)\":$")
    public void applicationDoesnTReceiveTheFollowingBlocksFromHimForTorrent(String torrentFileName,
                                                                            List<BlockOfPiece> notExpectedBlockFromFakePeerList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // I'm implicitly assuming that the allocator of the fake-peer is big enough
        // to send me back exactly the blocks I want with the specified size I wanted.
        List<BlockOfPiece> notExpectedBlockFromFakePeerListFixed = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(notExpectedBlockFromFakePeerList)
                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .collectList()
                .block();

        this.actualIncomingPieceMessages$.map(PieceEvent::getReceivedPiece)
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getAllocatedBlock().getLength()))
                .doOnNext(actualBlock -> Assert.assertFalse(notExpectedBlockFromFakePeerList.contains(actualBlock)))
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^fake-peer on port \"([^\"]*)\" choke me: \"([^\"]*)\" - for torrent: \"([^\"]*)\"$")
    public void fakePeerOnPortChokeMeForTorrent(Integer fakePeerPort, boolean isChoking, String torrentFileName) throws
            Throwable {
//        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
//
//        //noinspection UnassignedFluxMonoInstance
//        this.recordedFakePeersToMeLinks$.filter(remoteFakePeer -> remoteFakePeer.getMe().getPeerPort() == fakePeerPort)
//                .flatMap(remoteFakePeer -> (isChoking ?
//                        remoteFakePeer.sendMessages().sendChokeMessage() :
//                        remoteFakePeer.sendMessages().sendUnchokeMessage())
//                        .map(sendPeerMessages -> remoteFakePeer));
    }

    @Then("^application receive the following available pieces - for torrent: \"([^\"]*)\":$")
    public void applicationReceiveTheFollowingAvailablePiecesForTorrent(String torrentFileName,
                                                                        List<PeersContainingPiece> peersContainingPieces) throws Throwable {
        synchronized (this.fakePeersByPort) {
            this.linksByAvailableMissingPiece$.take(peersContainingPieces.size())
                    .timeout(Duration.ofSeconds(5))
                    .flatMap(linksContainingPiece -> {
                        List<Integer> expectedPeersContainingPiece = peersContainingPieces.stream()
                                .filter(peer -> peer.getPieceIndex() == linksContainingPiece.key())
                                .flatMap(peer -> peer.getPeers().stream())
                                .collect(Collectors.toList());
                        return linksContainingPiece.take(expectedPeersContainingPiece.size())
                                .timeout(Duration.ofSeconds(5))
                                .flatMap(link ->
                                {
                                    Mono<Integer> single = Flux.fromIterable(this.fakePeersByPort)
                                            .filter(fakePeerByPort -> fakePeerByPort.getValue().getLink().getPeer().equals(link.getMe()))
                                            .filter(fakePeerByPort -> fakePeerByPort.getValue().getLink().getMe().equals(link.getPeer()))
                                            .filter(fakePeerByPort -> fakePeerByPort.getValue().getLink().getTorrentInfo().equals(link.getTorrentInfo()))
                                            .map(integerRemoteFakePeerSimpleEntry -> integerRemoteFakePeerSimpleEntry.getKey())
                                            .single();
                                    return single;
                                })
                                .collectList()
                                .doOnNext(actualPeersContainingPiece -> Utils.assertListEqualNotByOrder(expectedPeersContainingPiece, actualPeersContainingPiece, Integer::equals));
                    })
                    .collectList()
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
        }

        this.linksByAvailableMissingPiece$ = this.peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                .replay()
                .autoConnect(0);
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
        synchronized (this.fakePeersByPort) {
            this.peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                    .filter(linksByPiece$ -> linksByPiece$.key() == pieceIndex)
                    .flatMap(linksByPiece$ -> linksByPiece$.map(Function.identity()))
                    .flatMap(link -> Flux.fromIterable(this.fakePeersByPort)
                            .filter(fakePeerByPort -> fakePeerByPort.getValue().getLink().equals(link)))
                    .map(AbstractMap.SimpleEntry::getKey)
                    .timeout(Duration.ofMillis(50), Mono.empty())
                    .collectList()
                    .doOnNext(actualPeersWithPiece -> Utils.assertListEqualNotByOrder(expectedAvailableFakePeerPortList, actualPeersWithPiece, Integer::equals))
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
        }
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
    public void applicationReceiveNoneAvailableFakePeersForPieceForTorrent(int pieceIndex, String torrentFileName) {

    }

    @Then("^application receive the none available pieces - for torrent: \"([^\"]*)\"$")
    public void applicationReceiveTheNoneAvailablePiecesForTorrent(String torrentFileName) {

    }

    @Then("^application download the following pieces - concurrent piece's downloads: \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadTheFollowingPiecesConcurrentPieceSDownloadsForTorrent(
            int concurrentPieceDownloads,
            String torrentFileName,
            List<Integer> piecesToDownloadList) {

    }

    @Then("^application downloaded the following pieces - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadedTheFollowingPiecesForTorrent(String torrentFileName,
                                                                  List<Integer> piecesDownloadedList) {

    }

    @Then("^application couldn't downloaded the following pieces - for torrent: \"([^\"]*)\":$")
    public void applicationCloudnTDownloadedTheFollowingPiecesForTorrent(String torrentFileName,
                                                                         List<Integer> piecesNotDownloadedList) {

    }

    @And("^the saved-pieces-flux send complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedPiecesFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) {
    }

    @And("^the saved-blocks-flux send  complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedBlocksFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) {
    }

    @Then("^torrent-status change: \"([^\"]*)\" and notify only about the changes - for torrent \"([^\"]*)\":$")
    public void torrentStatusChangeAndNotifyOnlyAboutTheChangesForTorrent(TorrentStatusAction
                                                                                  torrentStatusActionChanging, String torrentFileName) {

    }


    @Given("^allocator for \"([^\"]*)\" blocks with \"([^\"]*)\" bytes each$")
    public void allocatorForBlocksWithBytesEach(int amountOfBlocksToAllocate, int blockLength) {
        Utils.removeEverythingRelatedToLastTest();

        TorrentDownloaders.getAllocatorStore().updateAllocations(amountOfBlocksToAllocate, blockLength)
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getAmountOfBlocks()))
                .doOnNext(allocatorState -> Assert.assertEquals(blockLength, allocatorState.getBlockLength()))
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getAllocatedBlocks().length))
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getFreeBlocksStatus().cardinality()))
                .doOnNext(allocatorState -> {
                    BitSet allBlocksAreFreeStatus = new BitSet(amountOfBlocksToAllocate);
                    allBlocksAreFreeStatus.set(0, amountOfBlocksToAllocate, true);
                    Assert.assertEquals(allBlocksAreFreeStatus, allocatorState.getFreeBlocksStatus());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Given("^allocator for \"([^\"]*)\" blocks with allocated-block-length which is bigger than piece: \"([^\"]*)\" for torrent: \"([^\"]*)\":$")
    public void allocatorForBlocksWithAllocatedBlockLengthWhichIsBiggerThanPieceForTorrent(
            int amountOfBlocksToAllocate,
            int pieceIndex,
            String torrentFileName) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        int pieceLength = torrentInfo.getPieceLength(pieceIndex);

        TorrentDownloaders.getAllocatorStore().updateAllocations(amountOfBlocksToAllocate, pieceLength)
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getAmountOfBlocks()))
                .doOnNext(allocatorState -> Assert.assertEquals(pieceLength, allocatorState.getBlockLength()))
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getAllocatedBlocks().length))
                .doOnNext(allocatorState -> Assert.assertEquals(amountOfBlocksToAllocate, allocatorState.getFreeBlocksStatus().cardinality()))
                .doOnNext(allocatorState -> {
                    BitSet allBlocksAreFreeStatus = new BitSet(amountOfBlocksToAllocate);
                    allBlocksAreFreeStatus.set(0, amountOfBlocksToAllocate, true);
                    Assert.assertEquals(allBlocksAreFreeStatus, allocatorState.getFreeBlocksStatus());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    private List<PieceMessage> actualPieceMessageList;


    @When("^we create the following piece-messages from \"([^\"]*)\" threads for torrent: \"([^\"]*)\":$")
    public void weCreateTheFollowingPieceMessagesFromThreadsForTorrent(int threadsAmount, String torrentFileName,
                                                                       List<BlockOfPiece> blockOfPieceList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Flux<AllocatorState> allocatorStateFlux = TorrentDownloaders.getAllocatorStore().states$()
                .replay(blockOfPieceList.size())
                .autoConnect(0);

        this.actualPieceMessageList = TorrentDownloaders.getAllocatorStore().latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(blockOfPieceList)
                                .map(oldBlockOfPiece -> {
                                    BlockOfPiece fixedBlockOfPiece = Utils.fixBlockOfPiece(oldBlockOfPiece, torrentInfo, allocatedBlockLength);
                                    System.out.println("oldBlockOfPiece: " + oldBlockOfPiece);
                                    System.out.println("fixedBlockOfPiece: " + fixedBlockOfPiece);
                                    System.out.println("------------");
                                    return fixedBlockOfPiece;
                                }))
                .flatMap(blockOfPiece ->
                        {
                            int pieceIndex = blockOfPiece.getPieceIndex();
                            Integer from = blockOfPiece.getFrom();
                            Integer length = blockOfPiece.getLength();
                            int pieceLength = torrentInfo.getPieceLength(pieceIndex);
                            return TorrentDownloaders.getAllocatorStore()
                                    .createPieceMessage(null, null, pieceIndex, from, length, pieceLength)
                                    .subscribeOn(MyStepdefs.testParallelScheduler);
                        }
                        , threadsAmount)
                .doOnNext(pieceMessage -> System.out.println("created the following pieceMessage: " + pieceMessage))
                .collectList()
                .block();

//		// check that the states during the allocations are valid:
//
//		Flux.zip(allocatorStateFlux, Flux.fromIterable(this.actualPieceMessageList),
//				(allocatorState, pieceMessage) -> {
//					int blockIndex = pieceMessage.getAllocatedBlock().getBlockIndex();
//					Assert.assertEquals(allocatorState.getAllocatedBlocks()[blockIndex], pieceMessage.getAllocatedBlock());
//					Assert.assertFalse(allocatorState.getFreeBlocksStatus().get(blockIndex));
//					return pieceMessage;
//				}).as(StepVerifier::create)
//				.expectNextCount(blockOfPieceList.size())
//				.verifyComplete();
    }

    @Then("^we created the following piece-messages for torrent: \"([^\"]*)\":$")
    public void weCreatedTheFollowingPieceMessagesForTorrent(String torrentFileName,
                                                             List<BlockOfPiece> blockOfPieceList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        List<BlockOfPiece> expectedPieceMessageList = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(blockOfPieceList)
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .collectList()
                .block();

        List<BlockOfPiece> actualBlockOfPieceList = this.actualPieceMessageList.stream()
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(),
                        pieceMessage.getAllocatedBlock().getLength()))
                .collect(Collectors.toList());

        Utils.assertListEqualNotByOrder(expectedPieceMessageList, actualBlockOfPieceList, BlockOfPiece::equals);
    }

    private List<RequestMessage> actualRequestMessageList;

    @When("^we create the following request-messages from \"([^\"]*)\" threads for torrent: \"([^\"]*)\":$")
    public void weCreateTheFollowingRequestMessagesFromThreadsForTorrent(int threadsAmount, String
            torrentFileName,
                                                                         List<BlockOfPiece> blockOfPieceList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        this.actualRequestMessageList = TorrentDownloaders.getAllocatorStore().latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(blockOfPieceList)
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .flatMap(blockOfPiece -> {
                    int pieceIndex = blockOfPiece.getPieceIndex();
                    Integer from = blockOfPiece.getFrom();
                    Integer length = blockOfPiece.getLength();
                    int pieceLength = torrentInfo.getPieceLength(pieceIndex);
                    return TorrentDownloaders.getAllocatorStore()
                            .createRequestMessage(null, null, pieceIndex, from, length, pieceLength)
                            .subscribeOn(MyStepdefs.testParallelScheduler);
                }, threadsAmount)
                .collectList()
                .block();
    }

    @Then("^we created the following request-messages for torrent: \"([^\"]*)\":$")
    public void weCreatedTheFollowingRequestMessagesForTorrent(String torrentFileName,
                                                               List<BlockOfPiece> blockOfPieceList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        List<BlockOfPiece> expectedPieceMessageList = TorrentDownloaders.getAllocatorStore().latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(blockOfPieceList)
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .collectList()
                .block();

        List<BlockOfPiece> actualBlockOfPieceList = this.actualRequestMessageList.stream()
                .map(requestMessage -> new BlockOfPiece(requestMessage.getIndex(), requestMessage.getBegin(),
                        requestMessage.getBlockLength()))
                .collect(Collectors.toList());

        Utils.assertListEqualNotByOrder(expectedPieceMessageList, actualBlockOfPieceList, BlockOfPiece::equals);
    }

    @Then("^the allocator have \"([^\"]*)\" used blocks$")
    public void theAllocatorHaveUsedBlocks(long expectedUsedBlocksAmount) {
        TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .doOnNext(allocatorState -> {
                    long actualUsedBlocksAmount = IntStream.range(0, allocatorState.getAmountOfBlocks())
                            .filter(index -> !allocatorState.getFreeBlocksStatus().get(index))
                            .count();

                    Assert.assertEquals(expectedUsedBlocksAmount, actualUsedBlocksAmount);
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^the allocator have \"([^\"]*)\" free blocks$")
    public void theAllocatorHaveFreeBlocks(int expectedFreeBlocksAmount) {
        TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .doOnNext(allocatorState -> {
                    int actualUsedBlocksAmount = 0;
                    for (int i = 0; i < allocatorState.getAmountOfBlocks(); i++)
                        if (allocatorState.getFreeBlocksStatus().get(i))
                            actualUsedBlocksAmount++;
                    Assert.assertEquals(expectedFreeBlocksAmount, actualUsedBlocksAmount);
                }).as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^we free the following piece-messages for torrent: \"([^\"]*)\":$")
    public void weFreeTheFollowingPieceMessagesForTorrent(String torrentFileName,
                                                          List<BlockOfPiece> blockOfPieceList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore().latestState$()
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength ->
                        Flux.fromIterable(blockOfPieceList)
                                .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength)))
                .flatMap(blockOfPiece -> Flux.fromIterable(this.actualPieceMessageList)
                        .filter(pieceMessage -> pieceMessage.getIndex() == blockOfPiece.getPieceIndex())
                        .filter(pieceMessage -> pieceMessage.getBegin() == blockOfPiece.getFrom())
                        .filter(pieceMessage -> pieceMessage.getAllocatedBlock().getLength() == blockOfPiece.getLength())
                        .single())
                .flatMap(pieceMessage -> TorrentDownloaders.getAllocatorStore().free(pieceMessage.getAllocatedBlock())
                        .map(allocatorState -> allocatorState.getFreeBlocksStatus().get(pieceMessage.getAllocatedBlock().getBlockIndex()))
                        .doOnNext(isAllocationFreed ->
                                Assert.assertTrue("allocation wasn't freed: " + pieceMessage, isAllocationFreed)))
                .as(StepVerifier::create)
                .expectNextCount(blockOfPieceList.size())
                .verifyComplete();
    }

    @When("^we free all piece-messages for torrent: \"([^\"]*)\"$")
    public void weFreeAllPieceMessagesForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore()
                .freeAll()
                .doOnNext(allocatorState ->
                        IntStream.range(0, allocatorState.getAmountOfBlocks())
                                .forEach(index -> Assert.assertTrue("we freed all blocks but" +
                                                " this index is not freed: " + index + " in state: " + allocatorState,
                                        allocatorState.getFreeBlocksStatus().get(index))))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Given("^initial torrent-status for torrent: \"([^\"]*)\" in \"([^\"]*)\" with default initial state$")
    public void initialTorrentStatusForTorrentInWithDefaultInitialState(String torrentFileName,
                                                                        String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;
        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.buildDefault(torrentInfo, "Test-App", fullDownloadPath);
        TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader);
    }

    @Given("^initial listen-status - default$")
    public void initialListenStatus() {
        logger.debug("starting test of listener-redux with side effects.");
        Utils.removeEverythingRelatedToLastTest();
        logger.debug("end cleaning up all reasources from last test of listener-redux with side effects.");
    }


    @Given("^initial listen-status - without dispaching anything - default$")
    public void initialListenStatusWithoutDispachingAnythingDefault() {
        logger.debug("starting test of listener-redux with side effects - without dispaching anything.");
        Utils.removeEverythingRelatedToLastTest();
        logger.debug("end cleaning up all reasources from last test of listener-redux with side effects - without dispaching anything.");
    }

    @When("^listen-status is trying to change to:$")
    public void listenStatusIsTryingToChangeTo(List<ListenerAction> changesActionList) {
        Utils.changeListenerState(changesActionList, TorrentDownloaders.getListenStore());
    }

    @Then("^listen-status will change to: \"([^\"]*)\":$")
    public void listenStatusWillBeWithAction(ListenerAction lastAction, List<ListenerAction> expectedActionList) {
        ListenerState expectedState = Utils.getListenStatusState(lastAction, expectedActionList);

        TorrentDownloaders.getListenStore()
                .latestState$()
                .doOnNext(actualState -> Assert.assertEquals(expectedState.getAction(), actualState.getAction()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningInProgress(), actualState.isStartedListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningSelfResolved(), actualState.isStartedListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningWindUp(), actualState.isStartedListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningInProgress(), actualState.isResumeListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningSelfResolved(), actualState.isResumeListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningWindUp(), actualState.isResumeListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningInProgress(), actualState.isPauseListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningSelfResolved(), actualState.isPauseListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningWindUp(), actualState.isPauseListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningInProgress(), actualState.isRestartListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningSelfResolved(), actualState.isRestartListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningWindUp(), actualState.isRestartListeningWindUp()))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        Utils.removeEverythingRelatedToLastTest();
    }

    private Store<ListenerState, ListenerAction> listenStore;

    @Given("^initial listen-status - no side effects:$")
    public void initialListenStatusNoSideEffects(List<ListenerAction> initialStateByActionList) {
        Utils.removeEverythingRelatedToLastTest();

        ListenerState initialState = Utils.getListenStatusState(ListenerAction.INITIALIZE, initialStateByActionList);
        this.listenStore = new Store<>(new ListenerReducer(), initialState, "Test-App-Listener-Store-No-Side-Effects");
    }

    @Given("^initial listen-status - without dispaching anything - no side effects:$")
    public void initialListenStatusWithoutDispachingAnythingNoSideEffects
            (List<ListenerAction> initialStateByActionList) {
        ListenerState initialState = Utils.getListenStatusState(ListenerAction.INITIALIZE, initialStateByActionList);
        this.listenStore = new Store<>(new ListenerReducer(), initialState, "Test-App-Listener-Store-No-Side-Effects");
    }

    @When("^listen-status is trying to change to - no side effects:$")
    public void listenStatusIsTryingToChangeToNoSideEffects(List<ListenerAction> changesActionList) {
        Flux.fromIterable(changesActionList)
                .flatMap(listenerAction -> this.listenStore.dispatch(listenerAction),
                        changesActionList.size())
                .as(StepVerifier::create)
                .expectNextCount(changesActionList.size())
                .verifyComplete();
        ;
    }

    @When("^listen-status is trying to change \"([^\"]*)\" when it can and also - no side effects:$")
    public void listenStatusIsTryingToChangeWhenItCanAndAlsoNoSideEffects(ListenerAction listenerAction,
                                                                          List<ListenerAction> changesActionList) {
        BiPredicate<ListenerAction, ListenerState> isInitialized = (desiredChange, listenerState) ->
                ListenerReducer.defaultListenState.getAction().equals(listenerState.getAction()) &&
                        ListenerReducer.defaultListenState.isStartedListeningInProgress() == listenerState.isStartedListeningInProgress() &&
                        ListenerReducer.defaultListenState.isStartedListeningSelfResolved() == listenerState.isStartedListeningSelfResolved() &&
                        ListenerReducer.defaultListenState.isStartedListeningWindUp() == listenerState.isStartedListeningWindUp() &&
                        ListenerReducer.defaultListenState.isResumeListeningInProgress() == listenerState.isResumeListeningInProgress() &&
                        ListenerReducer.defaultListenState.isResumeListeningSelfResolved() == listenerState.isResumeListeningSelfResolved() &&
                        ListenerReducer.defaultListenState.isResumeListeningWindUp() == listenerState.isResumeListeningWindUp() &&
                        ListenerReducer.defaultListenState.isPauseListeningInProgress() == listenerState.isPauseListeningInProgress() &&
                        ListenerReducer.defaultListenState.isPauseListeningSelfResolved() == listenerState.isPauseListeningSelfResolved() &&
                        ListenerReducer.defaultListenState.isPauseListeningWindUp() == listenerState.isPauseListeningWindUp() &&
                        ListenerReducer.defaultListenState.isRestartListeningInProgress() == listenerState.isRestartListeningInProgress() &&
                        ListenerReducer.defaultListenState.isRestartListeningSelfResolved() == listenerState.isRestartListeningSelfResolved() &&
                        ListenerReducer.defaultListenState.isRestartListeningWindUp() == listenerState.isRestartListeningWindUp();

        BiPredicate<ListenerAction, ListenerState> isRestartedOrRestarting = (desiredChange, listenerState) ->
                listenerState.fromAction(ListenerAction.RESTART_LISTENING_IN_PROGRESS) ||
                        listenerState.fromAction(ListenerAction.RESTART_LISTENING_WIND_UP);

        BiPredicate<ListenerAction, ListenerState> isCorrespondingIsProgressCanceled = (desiredChange, listenerState) ->
                !listenerState.fromAction(ListenerAction.getCorrespondingIsProgressAction(desiredChange));

        BiPredicate<ListenerAction, ListenerState> didWeAlreadySucceed = (desiredChange, listenerState) ->
                listenerState.fromAction(desiredChange);

        BiPredicate<ListenerAction, ListenerState> isCanceled = isInitialized.or(isRestartedOrRestarting)
                .or(didWeAlreadySucceed).or(isCorrespondingIsProgressCanceled)
                .negate();

        Mono<List<ListenerState>> changeTo$ = Flux.fromIterable(changesActionList)
                .flatMap(action -> this.listenStore.dispatch(action), changesActionList.size())
                .collectList();

        Mono<ListenerState> listenerStateMono = this.listenStore.tryDispatchUntil(listenerAction, isCanceled)
                .publishOn(MyStepdefs.testParallelScheduler)
                .defaultIfEmpty(ListenerReducer.defaultListenState);
        Flux.merge(listenerStateMono, changeTo$.publishOn(MyStepdefs.testParallelScheduler))
                .blockLast();
    }

    @Then("^listen-status will change to: \"([^\"]*)\" - no side effects:$")
    public void listenStatusWillChangeToNoSideEffects(ListenerAction
                                                              lastAction, List<ListenerAction> expectedActionList) {
        ListenerState expectedState = Utils.getListenStatusState(lastAction, expectedActionList);
        this.listenStore.latestState$()
                .doOnNext(actualState -> Assert.assertEquals(expectedState.getAction(), actualState.getAction()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningInProgress(), actualState.isStartedListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningSelfResolved(), actualState.isStartedListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isStartedListeningWindUp(), actualState.isStartedListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningInProgress(), actualState.isResumeListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningSelfResolved(), actualState.isResumeListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isResumeListeningWindUp(), actualState.isResumeListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningInProgress(), actualState.isPauseListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningSelfResolved(), actualState.isPauseListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isPauseListeningWindUp(), actualState.isPauseListeningWindUp()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningInProgress(), actualState.isRestartListeningInProgress()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningSelfResolved(), actualState.isRestartListeningSelfResolved()))
                .doOnNext(actualState -> Assert.assertEquals(expectedState.isRestartListeningWindUp(), actualState.isRestartListeningWindUp()))
                .block();

        this.listenStore.dispose();
        Utils.removeEverythingRelatedToLastTest();
    }

    @When("^fake-peer on port \"([^\"]*)\" try to connect for torrent \"([^\"]*)\", he receive the following error: \"([^\"]*)\"$")
    public void fakePeerOnPortTryToConnectForTorrentHeReceiveTheFollowingError(String fakePeerPort, String torrentFileName, String exceptionType) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Throwable throwable;
        switch (exceptionType) {
            case "TimeoutException":
                throwable = new TimeoutException();
                break;
            default:
                throw new InvalidParameterException("the given exception type is not supported in this test case");
        }

        Flux<Link> appToFakePeerLink$ = TorrentDownloaders.getListener()
                .getPeers$(torrentInfo)
                .replay()
                .autoConnect(0);

        Mono<Link> publisher = TorrentDownloaders.getListener()
                .getListeningPort()
                .timeout(Duration.ofSeconds(2), Mono.just(12345))
                .map(listeningPort -> new Peer("localhost", listeningPort))
                .flatMap(app -> {
                    EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessagesFakePeer$ = EmitterProcessor.create();
                    FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessagesFakePeer = incomingPeerMessagesFakePeer$.sink();
                    return new PeersProvider(MyStepdefs.globalFakePeerAllocator, torrentInfo, "Fake peer", emitIncomingPeerMessagesFakePeer).connectToPeerMono(app);
                })
                .doOnNext(Link::dispose);
        if (PeerExceptions.peerNotResponding.test(throwable))
            StepVerifier.create(publisher)
                    .verifyComplete();
        else
            StepVerifier.create(publisher)
                    .expectError(throwable.getClass())
                    .verify();

        appToFakePeerLink$.doOnNext(Link::dispose)
                .timeout(Duration.ofMillis(100), Mono.empty())
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^fake-peer on port \"([^\"]*)\" try to connect for torrent \"([^\"]*)\", he succeed$")
    public void fakePeerOnPortTryToConnectForTorrentHeSucceed(String fakePeerPort, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        Flux<Link> linkFlux = TorrentDownloaders.getListener()
                .getPeers$(torrentInfo)
                .replay()
                .autoConnect(0);

        Flux<Link> appToFakePeerLink$ = TorrentDownloaders.getListener()
                .getPeers$(torrentInfo)
                .replay()
                .autoConnect(0);

        TorrentDownloaders.getListener()
                .getListeningPort()
                .map(listeningPort -> new Peer("localhost", listeningPort))
                .flatMap(app -> {
                    EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessagesFakePeer$ = EmitterProcessor.create();
                    FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessagesFakePeer = incomingPeerMessagesFakePeer$.sink();
                    PeersProvider peersProvider = new PeersProvider(MyStepdefs.globalFakePeerAllocator, torrentInfo, "Fake peer", emitIncomingPeerMessagesFakePeer);
                    return peersProvider.connectToPeerMono(app);
                })
                .doOnNext(Link::dispose)
                .flatMap(__ -> linkFlux.take(1).single())
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        appToFakePeerLink$.doOnNext(Link::dispose)
                .timeout(Duration.ofMillis(100), Mono.empty())
                .collectList()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @When("^torrent-status for torrent \"([^\"]*)\" is trying to change to:$")
    public void torrentStatusForTorrentIsTryingToChangeTo(String torrentFileName,
                                                          List<TorrentStatusAction> changeTorrentStatusActionList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore();

        Flux.fromIterable(changeTorrentStatusActionList)
                .flatMap(torrentStatusStore::dispatch, changeTorrentStatusActionList.size(), changeTorrentStatusActionList.size())
                .as(StepVerifier::create)
                .expectNextCount(changeTorrentStatusActionList.size())
                .verifyComplete();
    }

    @Then("^wait until action is: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void waitUntilActionIsForTorrent(TorrentStatusAction torrentStatusAction, String torrentFileName) throws
            Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore()
                .states$()
                .takeUntil(torrentStatusState -> torrentStatusState.fromAction(torrentStatusAction))
                .last()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^start search and receive \"([^\"]*)\" peers from search-module for torrent: \"([^\"]*)\"$")
    public void startSearchAndReceivePeersFromSearchModuleForTorrent(int numberOfPeers, String torrentFileName) throws
            Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        System.out.println("trying to connect to couple of peers in torrent: " + torrentInfo);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        torrentDownloader.getSearchPeers()
                .getPeers$()
                .take(numberOfPeers)
                .doOnSubscribe(__ -> torrentDownloader.getTorrentStatusStore().dispatchNonBlocking(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS))
                .as(StepVerifier::create)
                .expectNextCount(numberOfPeers)
                .verifyComplete();
    }

    @Then("^wait until state contain the following for torrent: \"([^\"]*)\":$")
    public void waitUntilStateContainTheFollowingForTorrent(String torrentFileName,
                                                            List<TorrentStatusAction> actionsToWaitFor) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore()
                .states$()
                .takeUntil(torrentStatusState -> actionsToWaitFor.stream().allMatch(torrentStatusState::fromAction))
                .last()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^torrent-status for torrent \"([^\"]*)\" will be:$")
    public void torrentStatusForWillBe(String torrentFileName,
                                       List<TorrentStatusAction> expectedTorrentStatusActionList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentStatusState expectedState = Utils.getTorrentStatusState(torrentInfo,
                // the action is not apart of the test because I can't know what it will be.
                // so I randomly choose TorrentStatusAction.INITIALIZE.
                TorrentStatusAction.INITIALIZE, expectedTorrentStatusActionList);

        TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getTorrentStatusStore()
                .latestState$()
                .doOnNext(torrentStatusState -> {
                    Assert.assertEquals(expectedState.getDownloadState(), torrentStatusState.getDownloadState());
                    Assert.assertEquals(expectedState.getTorrentFileSystemState(), torrentStatusState.getTorrentFileSystemState());
                    Assert.assertEquals(expectedState.getSearchPeersState(), torrentStatusState.getSearchPeersState());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application receive the following available pieces - for torrent: \"([^\"]*)\": - none$")
    public void applicationReceiveTheFollowingAvailablePiecesForTorrentNone(String torrentFileName) throws Throwable {
        this.peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                .map(GroupedFlux::key)
                .timeout(Duration.ofMillis(50), Mono.empty())
                .collectList()
                .doOnNext(actualAvailablePieces -> Assert.assertTrue(actualAvailablePieces.isEmpty()))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Then("^application receive the following available fake-peers for piece: \"([^\"]*)\" - for torrent: \"([^\"]*)\": - none$")
    public void applicationReceiveTheFollowingAvailableFakePeersForPieceForTorrentNone(int pieceIndex, String torrentFileName) throws Throwable {
        synchronized (this.fakePeersByPort) {
            this.peersToPiecesMapper.getLinksByAvailableMissingPiece$()
                    .filter(linksByPiece$ -> linksByPiece$.key() == pieceIndex)
                    .flatMap(linksByPiece$ -> linksByPiece$.map(Function.identity()))
                    .flatMap(link -> Flux.fromIterable(this.fakePeersByPort)
                            .filter(fakePeerByPort -> fakePeerByPort.getValue().getLink().equals(link)))
                    .map(AbstractMap.SimpleEntry::getKey)
                    .timeout(Duration.ofMillis(50), Mono.empty())
                    .collectList()
                    .doOnNext(actualPeersWithPiece -> Assert.assertTrue(actualPeersWithPiece.isEmpty()))
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
        }
    }

    @When("^application request something from all fake-peer on port \"([^\"]*)\" - for torrent: \"([^\"]*)\"$")
    public void applicationRequestSomethingFromAllFakePeerOnPortForTorrent(int fakePeerPort, String torrentFileName) throws Throwable {
        synchronized (this.fakePeersByPort) {
            Flux.fromIterable(this.fakePeersByPort)
                    .filter(fakePeerByPort -> fakePeerByPort.getKey() == fakePeerPort)
                    .map(AbstractMap.SimpleEntry::getValue)
                    .map(RemoteFakePeer::getLink)
                    .flatMap(link -> link.sendMessages().sendRequestMessage(0, 0, 1))
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
        }
    }

    @Given("^the following saved pieces - for torrent: \"([^\"]*)\": - none$")
    public void theFollowingSavedPiecesForTorrentNone(String torrentFileName) throws Throwable {

    }

    @Given("^the following saved pieces - for torrent: \"([^\"]*)\":$")
    public void theFollowingSavedPiecesForTorrent(String torrentFileName, List<Integer> piecesToSave) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        System.out.println("trying to connect to couple of peers in torrent: " + torrentInfo);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        torrentDownloader.getAllocatorStore()
                .updateAllocations(10, torrentInfo.getPieceLength(0))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        logger.info("app updated the allocator");

        UploadAlgorithm uploadAlgorithm = new UploadAlgorithmImpl(torrentInfo,
                torrentDownloader.getTorrentStatusStore(),
                torrentDownloader.getFileSystemLink(),
                torrentDownloader.getIncomingPeerMessagesNotifier()
                        .getIncomingPeerMessages$()
                        .filter(tuple2 -> tuple2.getValue() instanceof RequestMessage)
                        .map(tuple2 -> new AbstractMap.SimpleEntry<>(tuple2.getKey(), (RequestMessage) (tuple2.getValue()))));

        torrentDownloader.getTorrentStatusStore()
                .dispatchNonBlocking(TorrentStatusAction.START_UPLOAD_IN_PROGRESS);
        torrentDownloader.getTorrentStatusStore()
                .notifyWhen(TorrentStatusAction.RESUME_UPLOAD_WIND_UP)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        logger.info("app started uploading");

        Flux<Integer> savedPieces$ = torrentDownloader.getFileSystemLink()
                .savedPieces$()
                .replay()
                .autoConnect(0);

        Mono<Link> fakePeerToAppLink$ = TorrentDownloaders.getListener()
                .getListeningPort()
                .timeout(Duration.ofSeconds(2), Mono.just(12345))
                .map(listeningPort -> new Peer("localhost", listeningPort))
                .flatMap(app -> {
                    EmitterProcessor<AbstractMap.SimpleEntry<Link, PeerMessage>> incomingPeerMessagesFakePeer$ = EmitterProcessor.create();
                    FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessagesFakePeer = incomingPeerMessagesFakePeer$.sink();
                    return new PeersProvider(MyStepdefs.globalFakePeerAllocator, torrentInfo, "Fake peer", emitIncomingPeerMessagesFakePeer).connectToPeerMono(app);
                })
                .cache();

        fakePeerToAppLink$.flatMap(link ->
                MyStepdefs.globalFakePeerAllocator.latestState$()
                        .flatMapMany(allocatorState ->
                                Flux.fromIterable(piecesToSave)
                                        .flatMap(pieceToSave ->
                                                MyStepdefs.globalFakePeerAllocator
                                                        .createPieceMessage(link.getMe(), link.getPeer(), pieceToSave, 0, torrentInfo.getPieceLength(pieceToSave), allocatorState.getBlockLength())))
                        .publishOn(MyStepdefs.testParallelScheduler)
                        .flatMap(pieceMessage ->
                                        link.sendMessages().sendPieceMessage(pieceMessage)
                                                .flatMap(__ -> savedPieces$.filter(pieceIndex -> pieceIndex == pieceMessage.getIndex()).take(1).single())
                                , piecesToSave.size())
                        .collectList()
                        .doOnNext(__ -> link.dispose()))
                .doOnNext(__ -> torrentDownloader.getTorrentStatusStore().dispatchNonBlocking(TorrentStatusAction.PAUSE_UPLOAD_IN_PROGRESS))
                .flatMap(__ -> torrentDownloader.getTorrentStatusStore().notifyWhen(TorrentStatusAction.PAUSE_UPLOAD_WIND_UP))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    private Flux<List<Integer>> downloadPieces$;

    @Then("^application download the following pieces - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadTheFollowingPiecesForTorrent(String torrentFileName, List<Integer> expectedPiecesToDownload) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        TorrentDownloader torrentDownloader = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        List<Integer> fixedExpectedPiecesToDownload = expectedPiecesToDownload.stream()
                .map(pieceToDownload -> pieceToDownload >= 0 ?
                        pieceToDownload :
                        torrentInfo.getPieces().size() + pieceToDownload)
                .collect(Collectors.toList());

        PieceDownloader pieceDownloader = new PieceDownloaderImpl(torrentDownloader.getAllocatorStore(), torrentInfo, torrentDownloader.getFileSystemLink(),
                new BlockDownloaderImpl(torrentInfo, torrentDownloader.getFileSystemLink(), torrentDownloader.getIdentifier()));

        this.downloadPieces$ = this.linksByAvailableMissingPiece$.flatMap(peersByPiece$ ->
                        Flux.fromIterable(fixedExpectedPiecesToDownload)
                                .map(pieceToDownload -> pieceToDownload >= 0 ?
                                        pieceToDownload :
                                        torrentInfo.getPieces().size() + pieceToDownload)
                                .filter(pieceIndex -> pieceIndex.equals(peersByPiece$.key()))
                                .flatMap(__ -> pieceDownloader.downloadPiece$(peersByPiece$.key(), peersByPiece$.replay().autoConnect(0)))
                , fixedExpectedPiecesToDownload.size(), fixedExpectedPiecesToDownload.size())
                .take(fixedExpectedPiecesToDownload.size())
                .collectList()
                .doOnNext(actualDownloadedPieces -> Utils.assertListEqualNotByOrder(fixedExpectedPiecesToDownload, actualDownloadedPieces, Integer::equals))
                .flux()
                .replay()
                .autoConnect(0);
    }

    @Then("^wait until download is finished - for torrent: \"([^\"]*)\"$")
    public void waitUntilDownloadIsFinishedForTorrent(String torrentFileName) throws Throwable {
        this.downloadPieces$.as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        if (this.meToFakePeerLink$ != null) {
            this.meToFakePeerLink$.doOnNext(Link::dispose)
                    .timeout(Duration.ofMillis(100), Mono.empty())
                    .collectList()
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
            this.meToFakePeerLink$ = null;
        }

        this.fakePeersByPort.stream()
                .map(AbstractMap.SimpleEntry::getValue)
                .map(RemoteFakePeer::getLink)
                .forEach(Link::dispose);

        Utils.removeEverythingRelatedToLastTest();
    }

    @Then("^fake-peers disconnect- for torrent: \"([^\"]*)\"$")
    public void fakePeersDisconnectForTorrent(String torrentFileName) throws Throwable {
        if (this.meToFakePeerLink$ != null) {
            this.meToFakePeerLink$.doOnNext(Link::dispose)
                    .timeout(Duration.ofMillis(100), Mono.empty())
                    .collectList()
                    .as(StepVerifier::create)
                    .expectNextCount(1)
                    .verifyComplete();
            this.meToFakePeerLink$ = null;
        }

        this.fakePeersByPort.stream()
                .map(AbstractMap.SimpleEntry::getValue)
                .map(RemoteFakePeer::getLink)
                .forEach(Link::dispose);

        Utils.removeEverythingRelatedToLastTest();
    }
}
