package com.steps;

import christophedetroyer.torrent.TorrentFile;
import com.utils.*;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import main.App;
import main.AppConfig;
import main.TorrentInfo;
import main.downloader.*;
import main.file.system.FileSystemLink;
import main.file.system.FileSystemLinkImpl;
import main.file.system.allocator.AllocatedBlock;
import main.file.system.allocator.AllocatorReducer;
import main.file.system.allocator.AllocatorState;
import main.file.system.allocator.AllocatorStore;
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
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class MyStepdefs {
    private static Logger logger = LoggerFactory.getLogger(MyStepdefs.class);

    static {
        Hooks.onErrorDropped(throwable -> {
        });

        // active debug mode in reactor
        Hooks.onOperatorDebug();

        Utils.removeEverythingRelatedToLastTest();
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
                                            return trackerConnection.announceMono(this.torrentInfo.getTorrentInfoHash(),
                                                    AppConfig.getInstance().findFreePort());
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

    @Then("^application send to \\[peer ip: \"([^\"]*)\", peer port: \"([^\"]*)\"] and receive the following messages for torrent: \"([^\"]*)\",\"([^\"]*)\":$")
    public void applicationSendToPeerIpPeerPortAndReceiveTheFollowingMessagesForTorrent(String fakePeerIp,
                                                                                        int fakePeerPort,
                                                                                        String torrentFileName,
                                                                                        String downloadLocation,
                                                                                        List<PeerFakeRequestResponse> peerFakeRequestResponses) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        Peer app = new Peer("localhost", TorrentDownloaders.getListener().getTcpPort());
        Peer fakePeer = new Peer(fakePeerIp, fakePeerPort);
        String fullDownloadPathForFakePeer = System.getProperty("user.dir") +
                File.separator + "fake-peer-download-folder" + File.separator;

        // create a torrent downloader and a torrent file and save
        // a complete piece in it so we can send it to fake peer.
        int pieceIndex = 3;
        int pieceLength = torrentInfo.getPieceLength(pieceIndex);
        int begin = 0;
        int blockLength = pieceLength;
        Flux<PieceMessage> fakePieceMessageToSave$ = TorrentDownloaders.getAllocatorStore()
                .updateAllocations(4, blockLength)
                .flatMap(allocatorState -> TorrentDownloaders.getAllocatorStore()
                        .createPieceMessage(fakePeer, app, pieceIndex, begin, blockLength, allocatorState.getBlockLength()))
                .doOnNext(pieceMessageToSave -> {
                    for (int i = 0; i < blockLength; i++)
                        pieceMessageToSave.getAllocatedBlock().getBlock()[i] = 11;
                })
                .flux();

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");

        Mono<FileSystemLink> fileSystemLink$ = FileSystemLinkImpl.create(torrentInfo,
                fullDownloadPath,
                TorrentDownloaders.getAllocatorStore(),
                torrentStatusStore,
                fakePieceMessageToSave$);

        // I'm using this object to send pieceMessages and Request messages to the real app.
        Mono<TorrentDownloader> torrentDownloader$ = TorrentDownloaderBuilder
                .builder(torrentInfo)
                .setTorrentStatusStore(torrentStatusStore)
                .setToDefaultSearchPeers()
                .setToDefaultTorrentStatesSideEffects()
                .setToDefaultPeersCommunicatorFlux()
                .setFileSystemLink$(fileSystemLink$)
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .cache();

        logger.debug("app listen to incoming connection of the fake peer: " + fakePeer);
        Mono<Link> meToFakePeerLink$ = TorrentDownloaders.getListener()
                .getPeers$(torrentInfo)
                .take(1)
                .single()
                .cache();

        // recorded incoming messages to the app from fake-peer:
        List<PeerMessageType> messageToReceive = peerFakeRequestResponses.stream()
                .map(PeerFakeRequestResponse::getReceiveMessageType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        Flux<? extends PeerMessage> recordedResponses$ =
                Flux.fromIterable(messageToReceive)
                        .doOnNext(peerMessageType -> logger.debug("start listen to incoming message from type: " + peerMessageType.name()))
                        .flatMap(peerMessageType ->
                                        meToFakePeerLink$.flatMapMany(link ->
                                                Utils.getSpecificMessageResponseFluxByMessageType(link, peerMessageType))
                                , messageToReceive.size())
                        .doOnNext(peerMessage -> logger.debug("App received message from fake peer: " + peerMessage))
                        // if we received a piece message, then free it's allocation.
                        .concatMap(peerMessage -> {
                            if (peerMessage instanceof PieceMessage) {
                                logger.debug("the message we received is PieceMessage and we free it's block: " + peerMessage);
                                return TorrentDownloaders.getAllocatorStore()
                                        .free(((PieceMessage) peerMessage).getAllocatedBlock())
                                        .doOnNext(__ -> logger.debug("App freed the PieceMessage block and the Allocator state now is: " + __))
                                        .map(allocatorState -> peerMessage);
                            }
                            return Mono.just(peerMessage);
                        })
                        .replay(messageToReceive.size())
                        // start record incoming messages from fake peer
                        .autoConnect(0);


        // GENERAL NOTE: we need to see that the fake-peer actually listen to
        // incoming messages before the app send to him messages.

        List<PeerMessageType> messageToSendList = peerFakeRequestResponses.stream()
                .map(PeerFakeRequestResponse::getSendMessageType)
                .collect(Collectors.toList());
        logger.debug("fake peer: " + fakePeer + " is trying to connect to the app.");
        Mono<RemoteFakePeerCopy2> fakePeerToApp$ = TorrentDownloaders.getListenStore()
                .dispatch(ListenerAction.START_LISTENING_IN_PROGRESS)
                .flatMap(__ -> TorrentDownloaders.getListenStore().notifyWhen(ListenerAction.RESUME_LISTENING_WIND_UP))
                // I need to add this torrent to the list of
                // my torrents before a peer connect to me about this torrent.
                .flatMap(__ -> torrentDownloader$)
                .map(__ -> new PeersProvider(new AllocatorStore(new Store<>(new AllocatorReducer(),
                        AllocatorReducer.defaultAllocatorState, "Test-Fake-Peer-" + fakePeerPort + "-Allocator-Store")), torrentInfo))
                .flatMap(peersProvider -> peersProvider.connectToPeerMono(app))
                .map(link -> new RemoteFakePeerCopy2(link, fullDownloadPathForFakePeer))
                .cache();

        Mono<?> fakePeerResponses$ = fakePeerToApp$
                .doOnNext(remoteFakePeerCopy2 -> logger.debug("start sending messages to fake-peer"))
                .flatMap(remoteFakePeerCopy2 -> Flux.fromIterable(messageToSendList)
                        .concatMap(peerMessageType -> torrentDownloader$.map(TorrentDownloader::getFileSystemLink)
                                .flatMap(fileSystemLink -> meToFakePeerLink$.flatMap(meToFakePeerLink ->
                                        Utils.sendFakeMessage(torrentInfo, fullDownloadPath, meToFakePeerLink, peerMessageType,
                                                fileSystemLink, pieceIndex, begin, blockLength, pieceLength))))
                        .collectList()
                        .map(List::size)
                        .doOnNext(actualAmountOfSentMessages -> Assert.assertEquals("we didn't send to the fake-peer all the messages.",
                                messageToSendList.size(), actualAmountOfSentMessages.longValue()))
                        .flatMapMany(__ -> recordedResponses$)
                        .map(PeerMessage::getClass)
                        .doOnNext(actualPeerMessageType -> {
                            // if I received something from fake-peer then I expect
                            // to receive it so I check it he gave me the right message.
                            Optional<? extends Class<? extends PeerMessage>> responseFromFakePeer = peerFakeRequestResponses.stream()
                                    .map(PeerFakeRequestResponse::getReceiveMessageType)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .map(PeerMessageType::getSignal)
                                    .filter(actualPeerMessageType::equals)
                                    .findFirst();
                            Assert.assertTrue("I received a message which I didn't expect to receive from fake-peer.",
                                    responseFromFakePeer.isPresent());
                        })
                        .take(messageToSendList.size())
                        .collectList()
                        .doOnNext(actualReceivedMessagesList -> Assert.assertEquals("we didn't receive all the messages from fake-peer.",
                                messageToSendList.size(), actualReceivedMessagesList.size())));

        if (peerFakeRequestResponses.size() == 3 && peerFakeRequestResponses.get(2).getErrorSignalType().isPresent())
            StepVerifier.create(fakePeerResponses$)
                    .verifyError(peerFakeRequestResponses.get(2).getErrorSignalType().get().getErrorSignal());
        else
            // no errors
            StepVerifier.create(fakePeerResponses$)
                    .expectNextCount(1)
                    .verifyComplete();

        fakePeerToApp$.doOnNext(remoteFakePeerCopy2 -> logger.debug("clean up fake-peer resources."))
                .doOnNext(RemoteFakePeerCopy2::dispose)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        logger.debug("start clean up test resources.");
        Utils.removeEverythingRelatedToLastTest();
    }

    @Then("^application interested in all peers for torrent: \"([^\"]*)\"$")
    public void applicationInterestedInAllPeersForTorrent(String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
    }

    @When("^application create active-torrent for: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationCreateActiveTorrentFor(String torrentFileName, String downloadLocation) throws Throwable {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("started @When(\"^application create active-torrent for: \\\"([^\\\"]*)\\\",\\\"([^\\\"]*)\\\"$\")");
        System.out.println("-------------------------------------------------------------------------");
        Utils.removeEverythingRelatedToLastTest();
        System.out.println("finished clean after last test...");
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // this will waitForMessage an activeTorrent object.
        String downloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        TorrentDownloaderBuilder.builder(torrentInfo)
                .setToDefaultTorrentStatusStore("Test-App-TorrentStatus-Store")
                .setToDefaultTorrentStatesSideEffects()
                .setToDefaultSearchPeers()
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultFileSystemLink(downloadPath)
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @When(\"^application create active-torrent for: \\\"([^\\\"]*)\\\",\\\"([^\\\"]*)\\\"$\")");
        System.out.println("-------------------------------------------------------------------------");
    }

    @Then("^active-torrent exist: \"([^\"]*)\" for torrent: \"([^\"]*)\"$")
    public void activeTorrentExistForTorrent(boolean isActiveTorrentExist, String torrentFileName) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        // Note: this status is useless because we don't use ActiveTorrents class so there is no need to test anything.
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finsihed @Then(\"^active-torrent exist: \\\"([^\\\"]*)\\\" for torrent: \\\"([^\\\"]*)\\\"$\")");
        System.out.println("-------------------------------------------------------------------------");
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

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @Then(\"^files of torrent: \\\"([^\\\"]*)\\\" exist: \\\"([^\\\"]*)\\\" in \\\"([^\\\"]*)\\\"$\")");
        System.out.println("-------------------------------------------------------------------------");
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
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @Then(\"^application delete active-torrent: \\\"([^\\\"]*)\\\": \\\"([^\\\"]*)\\\" and file: \\\"([^\\\"]*)\\\"$\")");
        System.out.println("-------------------------------------------------------------------------");
    }

    @When("^application save the all the pieces of torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void applicationSaveTheAllThePiecesOfTorrent(String torrentFileName, String downloadLocation) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        TorrentDownloaders.getAllocatorStore()
                .updateAllocations(10, 1_000_000)
                .block();

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        TorrentStatesSideEffects sideEffects = new TorrentStatesSideEffects(torrentInfo, torrentStatusStore);

        // release new next signal only when we finish working on the last one and only after we cleaned it's buffer.
        int amountOfAllocatedBlocks = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .map(AllocatorState::getAmountOfBlocks)
                .block();

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
                                .flatMap(blockOfPiece -> Utils.createRandomPieceMessages(torrentInfo, semaphore, blockOfPiece, allocatedBlockLength)))
                        .doOnNext(pieceMessage -> System.out.println("saving: " + pieceMessage.getIndex() + "," + pieceMessage.getBegin() + "," + pieceMessage.getAllocatedBlock().getLength()))
                        .publishOn(App.MyScheduler)
                        .publish();

        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        TorrentDownloaderBuilder.builder(torrentInfo)
                .setTorrentStatusStore(torrentStatusStore)
                .setTorrentStatesSideEffects(sideEffects)
                .setFileSystemLink$(FileSystemLinkImpl.create(torrentInfo, fullDownloadPath, TorrentDownloaders.getAllocatorStore(), torrentStatusStore, allBlocksMessages$))
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .doOnNext(torrentDownloader -> {
                    Mono<List<Integer>> piecesCompleted1 = torrentDownloader.getFileSystemLink().savedBlockFlux()
                            //.doOnNext(pieceEvent -> System.out.println("block complete:" + pieceEvent))
                            .flatMap(pieceEvent ->
                            {
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

                    Mono<List<Integer>> piecesCompleted2 = torrentDownloader.getFileSystemLink().savedPieceFlux()
                            .replay()
                            .autoConnect(0)
                            .collectList();

                    allBlocksMessages$.connect();

                    List<Integer> expected = piecesCompleted1.block();
                    List<Integer> actual = piecesCompleted2.block();
                    Utils.assertListEqualNotByOrder(expected, actual, Integer::equals);
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    private Flux<Integer> actualCompletedSavedPiecesReadByFS$ = null;

    @When("^application save random blocks for torrent: \"([^\"]*)\" in \"([^\"]*)\" and check it saved$")
    public void applicationSaveARandomBlockInsideTorrentInAndCheckItSaved(String torrentFileName,
                                                                          String downloadLocation,
                                                                          List<BlockOfPiece> blockList) throws Throwable {
        // delete everything from the last test.
        Utils.removeEverythingRelatedToLastTest();

        TorrentDownloaders.getAllocatorStore()
                .freeAll()
                .flatMap(__ -> TorrentDownloaders.getAllocatorStore().updateAllocations(10, 1_000_000))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // I only need this object because the constructor of FileSystemLink listen to signals (which I never signal).
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        // I need this object for the cleanup after this test so I can wait until all the files were actually removed.
        TorrentStatesSideEffects sideEffects = new TorrentStatesSideEffects(torrentInfo, torrentStatusStore);

        // release new next signal only when we finish working on the last one and only after we cleaned it's buffer.
        Semaphore semaphore = new Semaphore(1, true);

        Flux<PieceMessage> generatedWrittenPieceMessages$ = TorrentDownloaders.getAllocatorStore()
                .latestState$()
                .publishOn(Schedulers.elastic())
                .map(AllocatorState::getBlockLength)
                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(blockList)
                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo, allocatedBlockLength))
                        .doOnNext(blockOfPiece -> System.out.println("start saving: " + blockOfPiece))
                        .flatMap((BlockOfPiece blockOfPiece) ->
                                Utils.createRandomPieceMessages(torrentInfo, semaphore, blockOfPiece, allocatedBlockLength)))
                .publishOn(App.MyScheduler)
                .publish()
                .autoConnect(2);

        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        Mono<TorrentDownloader> torrentDownloader$ = TorrentDownloaderBuilder.builder(torrentInfo)
                .setTorrentStatusStore(torrentStatusStore)
                .setTorrentStatesSideEffects(sideEffects)
                .setToDefaultSearchPeers()
                .setToDefaultPeersCommunicatorFlux()
                .setFileSystemLink$(FileSystemLinkImpl.create(torrentInfo, fullDownloadPath, TorrentDownloaders.getAllocatorStore(), torrentStatusStore, generatedWrittenPieceMessages$))
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .cache();

        // this.actualCompletedSavedPiecesReadByFS$ will be used in later step.
        this.actualCompletedSavedPiecesReadByFS$ = torrentDownloader$
                .map(TorrentDownloader::getFileSystemLink)
                .flatMapMany(FileSystemLink::savedPieceFlux)
                .replay()
                .autoConnect(0);

        Flux<PieceMessage> customPieces$ = torrentDownloader$
                .map(TorrentDownloader::getFileSystemLink)
                .flatMapMany(FileSystemLink::savedBlockFlux)
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
                        .flatMap(requestMessage -> Utils.readFromFile(torrentInfo, fullDownloadPath, requestMessage)
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
    public void applicationConnectToAllPeersAndAssertThatWeConnectedToThemForTorrent(String torrentFileName) throws Throwable {
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
//                            .flatMap(ReceiveMessagesNotifications::getPeerMessageResponseFlux)
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

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("started @Given(initial torrent-status for torrent:.... - no side effects)");
        System.out.println("-------------------------------------------------------------------------");
        Utils.removeEverythingRelatedToLastTest();
        System.out.println("finished clean the last test data");

        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // clean from the last test.
        TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        TorrentStatusState torrentStatusState = Utils.getTorrentStatusState(torrentInfo, TorrentStatusAction.INITIALIZE, torrentStatusActions);
        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore =
                new Store<>(new TorrentStatusReducer(), torrentStatusState, "Test-App-TorrentStatus-Store");

        TorrentDownloaderBuilder.builder(torrentInfo)
                .setTorrentStatusStore(torrentStatusStore)
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @Given(initial torrent-status for torrent:.... - no side effects)");
        System.out.println("-------------------------------------------------------------------------");
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

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @Then(\"^torrent-status for torrent \\\"([^\\\"]*)\\\" will be with action: \\\"([^\\\"]*)\\\" - no side effects:$\")");
        System.out.println("-------------------------------------------------------------------------");
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

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("finished @Then(\"^torrent-status for torrent \\\"([^\\\"]*)\\\" will be with action: \\\"([^\\\"]*)\\\":$\")");
        System.out.println("-------------------------------------------------------------------------");
    }

    private Mono<List<SendMessagesNotifications>> requestsFromFakePeerToMeList$;

    @Then("^random-fake-peer connect to me for torrent: \"([^\"]*)\" in \"([^\"]*)\" and he request:$")
    public void randomFakePeerConnectToMeForTorrentInAndHeRequest(String torrentFileName, String downloadLocation,
                                                                  List<BlockOfPiece> peerRequestBlockList) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        // The last step created FileSystemLinkImpl object which listener to custom
        // peerResponsesFlux. So I can't expect it to react to the original peerResponsesFlux.
        // Also the last test created torrentStatusController object.
        FileSystemLink fileSystemLink = TorrentDownloaders.getInstance()
                .findTorrentDownloader(torrentInfo.getTorrentInfoHash())
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."))
                .getFileSystemLink();

        // this flux is empty because if not, the application will get the peers from
        // them and then it will connect to all those peers and then those peers will
        // sendMessage me incoming messages and I don't want any incoming messages but the
        // messages from my fake-peer.
        Flux<TrackerConnection> trackerConnectionFlux = Flux.empty();

        TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentInfo.getTorrentInfoHash());

        // represent this application TorrentDownloader. (not the fake-peer TorrentDownloader).
        Store<TorrentStatusState, TorrentStatusAction> store = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        TrackerProvider trackerProvider = Mockito.mock(TrackerProvider.class);
        Mockito.when(trackerProvider.connectToTrackersFlux()).thenReturn(trackerConnectionFlux);
        Mono<TorrentDownloader> torrentDownloader$ = TorrentDownloaderBuilder.builder(torrentInfo)
                .setTorrentStatusStore(store)
                .setToDefaultTorrentStatesSideEffects()
                .setSearchPeers(new SearchPeers(torrentInfo, store, trackerProvider, new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo)))
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultBittorrentAlgorithm()
                .setFileSystemLink$(Mono.just(fileSystemLink))
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .flux()
                .replay(1)
                .autoConnect(0)
                .single();

        // start listen -> make fake peer connect to me -> send fake messages from fake-peer to me.
        this.requestsFromFakePeerToMeList$ = TorrentDownloaders.getListenStore()
                .dispatch(ListenerAction.START_LISTENING_IN_PROGRESS)
                .flatMap(__ -> store.dispatch(TorrentStatusAction.START_UPLOAD_IN_PROGRESS))
                .flatMap(__ -> TorrentDownloaders.getListenStore().notifyWhen(ListenerAction.START_LISTENING_WIND_UP))
                .flatMap(__ -> store.notifyWhen(TorrentStatusAction.RESUME_UPLOAD_WIND_UP))
                .flatMap(__ -> torrentDownloader$)
                .map(TorrentDownloader::getSearchPeers)
                .map(SearchPeers::getPeersProvider)
                .flatMap(peersProvider -> {
                    // the fake-peer will connect to me.
                    Peer me = new Peer("localhost", AppConfig.getInstance().findFreePort());
                    return peersProvider.connectToPeerMono(me);
                })
                .map(fakePeerToMeLink -> fakePeerToMeLink.sendMessages())
                .flatMapMany(sendMessagesObject -> sendMessagesObject.sendInterestedMessage()
                        .flatMapMany(__ -> TorrentDownloaders.getAllocatorStore()
                                .latestState$()
                                .map(AllocatorState::getBlockLength)
                                // sendMessage all requests from fake peer to me.
                                .flatMapMany(allocatedBlockLength -> Flux.fromIterable(peerRequestBlockList)
                                        .map(blockOfPiece -> Utils.fixBlockOfPiece(blockOfPiece, torrentInfo,
                                                allocatedBlockLength))))
                        .concatMap(blockOfPiece -> sendMessagesObject.sendRequestMessage(blockOfPiece.getPieceIndex(), blockOfPiece.getFrom(), blockOfPiece.getLength())))
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
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        Mono<Link> meToFakePeerLink = torrentDownloader.getPeersCommunicatorFlux()
                .take(1)
                .single()
                .cache();

        // I must record this because when I subscribe to this.requestsFromFakePeerToMeList$,
        // fake-peer will send me request messages and I response to him **piece messages**
        // which I don't want to lose.
        Flux<PieceMessage> recordedPieceMessageFlux = meToFakePeerLink
                .map(Link::sendMessages)
                .flatMapMany(SendMessagesNotifications::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class)
                .replay()
                .autoConnect(0);

        // send request massages from fake peer to me and get all the
        // piece messages from me to fake peer and collect them to list.
        Set<BlockOfPiece> actualBlockFromMeSet = this.requestsFromFakePeerToMeList$
                .flatMapMany(remoteFakePeerForRequestingPieces -> recordedPieceMessageFlux)
                .map(pieceMessage -> new BlockOfPiece(pieceMessage.getIndex(), pieceMessage.getBegin(),
                        pieceMessage.getAllocatedBlock().getLength()))
                .take(expectedBlockFromMeList.size())
                .collect(Collectors.toSet())
                .block();

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

        meToFakePeerLink.doOnNext(Link::closeConnection)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        Utils.removeEverythingRelatedToLastTest();
    }

    // TODO: is this step in used?
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

        Flux<TrackerConnection> trackers$ = Flux.<TrackerConnection>empty();
        String fullDownloadPath = System.getProperty("user.dir") + File.separator + downloadLocation + File.separator;

        Store<TorrentStatusState, TorrentStatusAction> torrentStatusStore = new Store<>(new TorrentStatusReducer(),
                TorrentStatusReducer.defaultTorrentState, "Test-App-TorrentStatus-Store");
        TrackerProvider trackerProvider = Mockito.mock(TrackerProvider.class);
        Mockito.when(trackerProvider.connectToTrackersFlux()).thenReturn(trackers$);
        // TODO: check that I didn't miss anything
        TorrentDownloaderBuilder.builder(torrentInfo)
                .setTorrentStatusStore(torrentStatusStore)
                .setToDefaultTorrentStatesSideEffects()
                .setSearchPeers(new SearchPeers(torrentInfo, torrentStatusStore, trackerProvider, new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo)))
                .setToDefaultPeersCommunicatorFlux()
                .setToDefaultFileSystemLink(fullDownloadPath)
                .build()
                .map(torrentDownloader -> TorrentDownloaders.getInstance().saveTorrentDownloader(torrentDownloader))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
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
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

        this.recordedMeToFakePeersLinks$ = torrentDownloader.getPeersCommunicatorFlux()
                // send interested message to the fake-peer.
                .flatMap(link -> link.sendMessages().sendInterestedMessage()
                        .map(sendPeerMessages -> link))
                .replay()
                .autoConnect(0);

        Peer me = new Peer("localhost", AppConfig.getInstance().findFreePort());
        Peer fakePeer = new Peer("localhost", fakePeerPort);

        // build a bitfield message so I can send it to my app and also
        // the fake-peer will update his status that he have those pieces.
        BitSet bitSet = new BitSet(torrentInfo.getPieces().size());
        fakePeerCompletedPieces.stream()
                .map(completedPieceIndex -> completedPieceIndex >= 0 ?
                        completedPieceIndex :
                        torrentInfo.getPieces().size() + completedPieceIndex)
                .forEach(completedPieceIndex -> bitSet.set(completedPieceIndex));

        Mono<RemoteFakePeer> fakePeerToMeLink$ = new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo)
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
//				.orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));
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
    public void fakePeerOnPortChokeMeForTorrent(Integer fakePeerPort, boolean isChoking, String torrentFileName) throws
            Throwable {
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
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

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
                .orElseThrow(() -> new IllegalStateException("torrent downloader object should have been created but it didn't."));

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
    public void applicationRequestAvailablePeersForPieceForTorrent(int pieceIndex, String torrentFileName) throws
            Throwable {

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
    public void applicationReceiveNoneAvailableFakePeersForPieceForTorrent(int pieceIndex, String torrentFileName) throws
            Throwable {

    }

    @Then("^application receive the none available pieces - for torrent: \"([^\"]*)\"$")
    public void applicationReceiveTheNoneAvailablePiecesForTorrent(String torrentFileName) throws Throwable {

    }

    @Then("^application download the following pieces - concurrent piece's downloads: \"([^\"]*)\" - for torrent: \"([^\"]*)\":$")
    public void applicationDownloadTheFollowingPiecesConcurrentPieceSDownloadsForTorrent(
            int concurrentPieceDownloads,
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

    @And("^the saved-pieces-flux send complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedPiecesFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) throws
            Throwable {
    }

    @And("^the saved-blocks-flux send  complete signal - for torrent: \"([^\"]*)\",\"([^\"]*)\"$")
    public void theSavedBlocksFluxSendCompleteSignalForTorrent(String torrentFileName, String downloadLocation) throws
            Throwable {
    }

    @Then("^torrent-status change: \"([^\"]*)\" and notify only about the changes - for torrent \"([^\"]*)\":$")
    public void torrentStatusChangeAndNotifyOnlyAboutTheChangesForTorrent(TorrentStatusAction
                                                                                  torrentStatusActionChanging, String torrentFileName) throws Throwable {

    }


    @Given("^allocator for \"([^\"]*)\" blocks with \"([^\"]*)\" bytes each$")
    public void allocatorForBlocksWithBytesEach(int amountOfBlocksToAllocate, int blockLength) throws Throwable {
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
                                    .subscribeOn(Schedulers.elastic());
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
                            .subscribeOn(Schedulers.elastic());
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
    public void theAllocatorHaveUsedBlocks(long expectedUsedBlocksAmount) throws Throwable {
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
    public void theAllocatorHaveFreeBlocks(int expectedFreeBlocksAmount) throws Throwable {
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
        TorrentDownloaderBuilder.buildDefault(torrentInfo, fullDownloadPath, "Test-App-TorrentStatus-Store")
                .map(TorrentDownloaders.getInstance()::saveTorrentDownloader)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Given("^initial listen-status - default$")
    public void initialListenStatus() throws Throwable {
        Utils.removeEverythingRelatedToLastTest();
    }


    @Given("^initial listen-status - without dispaching anything - default$")
    public void initialListenStatusWithoutDispachingAnythingDefault() throws Throwable {
    }

    @When("^listen-status is trying to change to:$")
    public void listenStatusIsTryingToChangeTo(List<ListenerAction> changesActionList) throws Throwable {
        Utils.changeListenerState(changesActionList, TorrentDownloaders.getListenStore());
    }

    @Then("^listen-status will change to: \"([^\"]*)\":$")
    public void listenStatusWillBeWithAction(ListenerAction lastAction, List<ListenerAction> expectedActionList) throws
            Throwable {
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
                .block();

        Utils.removeEverythingRelatedToLastTest();
    }

    private Store<ListenerState, ListenerAction> listenStore;

    @Given("^initial listen-status - no side effects:$")
    public void initialListenStatusNoSideEffects(List<ListenerAction> initialStateByActionList) throws Throwable {
        Utils.removeEverythingRelatedToLastTest();

        ListenerState initialState = Utils.getListenStatusState(ListenerAction.INITIALIZE, initialStateByActionList);
        this.listenStore = new Store<>(new ListenerReducer(), initialState, "Test-App-TorrentStatus-Store-No-Side-Effects");
    }

    @Given("^initial listen-status - without dispaching anything - no side effects:$")
    public void initialListenStatusWithoutDispachingAnythingNoSideEffects
            (List<ListenerAction> initialStateByActionList) throws Throwable {
        ListenerState initialState = Utils.getListenStatusState(ListenerAction.INITIALIZE, initialStateByActionList);
        this.listenStore = new Store<>(new ListenerReducer(), initialState, "Test-App-TorrentStatus-Store-No-Side-Effects");
    }

    @When("^listen-status is trying to change to - no side effects:$")
    public void listenStatusIsTryingToChangeToNoSideEffects(List<ListenerAction> changesActionList) throws
            Throwable {
        Flux.fromIterable(changesActionList)
                .flatMap(listenerAction -> this.listenStore.dispatch(listenerAction).publishOn(Schedulers.elastic()),
                        changesActionList.size())
                .blockLast();
    }

    @When("^listen-status is trying to change \"([^\"]*)\" when it can and also - no side effects:$")
    public void listenStatusIsTryingToChangeWhenItCanAndAlsoNoSideEffects(ListenerAction listenerAction,
                                                                          List<ListenerAction> changesActionList) throws Throwable {
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
                .flatMap(action -> this.listenStore.dispatch(action).publishOn(Schedulers.elastic()),
                        changesActionList.size())
                .collectList();

        Flux.merge(this.listenStore.tryDispatchUntil(listenerAction, isCanceled).publishOn(Schedulers.elastic())
                        .defaultIfEmpty(ListenerReducer.defaultListenState),
                changeTo$.publishOn(Schedulers.elastic()))
                .blockLast();
    }

    @Then("^listen-status will change to: \"([^\"]*)\" - no side effects:$")
    public void listenStatusWillChangeToNoSideEffects(ListenerAction
                                                              lastAction, List<ListenerAction> expectedActionList) throws Throwable {
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

        Utils.removeEverythingRelatedToLastTest();
    }

    @When("^fake-peer on port \"([^\"]*)\" try to connect for torrent \"([^\"]*)\", he receive the following error: \"([^\"]*)\"$")
    public void fakePeerOnPortTryToConnectForTorrentHeReceiveTheFollowingError(String fakePeerPort, String
            torrentFileName,
                                                                               String exceptionType) throws Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);

        Throwable throwable;
        switch (exceptionType) {
            case "TimeoutException":
                throwable = new TimeoutException();
                break;
            default:
                throw new InvalidParameterException("the given exception type is not supported in this test case");
        }

        Peer App = new Peer("localhost", TorrentDownloaders.getListener().getTcpPort());
        Mono<Link> publisher = new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo).connectToPeerMono(App)
                .doOnNext(Link::closeConnection);
        if (PeerExceptions.peerNotResponding.test(throwable))
            StepVerifier.create(publisher)
                    .verifyComplete();
        else
            StepVerifier.create(publisher)
                    .expectError(throwable.getClass())
                    .verify();


    }

    @When("^fake-peer on port \"([^\"]*)\" try to connect for torrent \"([^\"]*)\", he succeed$")
    public void fakePeerOnPortTryToConnectForTorrentHeSucceed(String fakePeerPort, String torrentFileName) throws
            Throwable {
        TorrentInfo torrentInfo = Utils.createTorrentInfo(torrentFileName);
        Flux<Link> linkFlux = TorrentDownloaders.getListener()
                .getPeers$(torrentInfo)
                .replay()
                .autoConnect(0);

        Peer App = new Peer("localhost", TorrentDownloaders.getListener().getTcpPort());
        Mono<Link> publisher = new PeersProvider(TorrentDownloaders.getAllocatorStore(), torrentInfo).connectToPeerMono(App)
                .doOnNext(Link::closeConnection);
        StepVerifier.create(publisher)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(linkFlux.take(1))
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
                .doOnNext(torrentStatusState -> System.out.println(torrentStatusState))
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
}
