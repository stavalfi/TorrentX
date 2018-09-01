package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PeersToPiecesMapper;
import main.algorithms.PieceDownloader;
import main.algorithms.PiecesDownloader;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

public class PiecesDownloaderImpl implements PiecesDownloader {
    private TorrentInfo torrentInfo;
    private Store<TorrentStatusState, TorrentStatusAction> store;
    private PeersToPiecesMapper peersToPiecesMapper;
    private FileSystemLink fileSystemLink;
    private PieceDownloader pieceDownloader;

    private Flux<Integer> downloadedPiecesFlux;
    private AllocatorStore allocatorStore;
    private Flux<TorrentStatusState> startDownload$;
    private Flux<TorrentStatusState> resumeDownload;
    private Flux<TorrentStatusState> pauseDownload;

    public PiecesDownloaderImpl(AllocatorStore allocatorStore,
                                TorrentInfo torrentInfo,
                                Store<TorrentStatusState, TorrentStatusAction> store,
                                FileSystemLink fileSystemLink,
                                PeersToPiecesMapper peersToPiecesMapper,
                                PieceDownloader pieceDownloader) {
        this.allocatorStore = allocatorStore;
        this.torrentInfo = torrentInfo;
        this.store = store;
        this.peersToPiecesMapper = peersToPiecesMapper;
        this.fileSystemLink = fileSystemLink;
        this.pieceDownloader = pieceDownloader;

        // TODO: note: if we ask for notification AFTER the download started, we will lose the notification.
//        downloadedPiecesFlux = store.states$()
//                // TODO: uncomment
//                //.filter(TorrentStatusState::isStartedDownload)
//                .take(1)
//                .flatMap(__ -> this.peersToPiecesMapper.getAvailablePieces$())
//                .flatMap(pieceIndex -> downloadPiece$(pieceIndex)
//                        // couldn't download a block from this piece in the specified time.
//                        // I will try to download the next piece.
//                        //.onErrorResume(TimeoutException.class, throwable -> Mono.empty())
//                        // concurrency = 1 -> how many pieces do I want to try to download concurrently.
//                        // prefetch = 1 -> the inner stream is a mono so any number is ok.
//                        , 1, 1)
//                .publish()
//                .autoConnect(0);
//
        this.startDownload$ = this.store.statesByAction(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> this.store.dispatch(TorrentStatusAction.START_DOWNLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.resumeDownload = this.store.statesByAction(TorrentStatusAction.RESUME_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> this.store.dispatch(TorrentStatusAction.RESUME_DOWNLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

        this.pauseDownload = this.store.statesByAction(TorrentStatusAction.PAUSE_DOWNLOAD_IN_PROGRESS)
                .concatMap(__ -> this.store.dispatch(TorrentStatusAction.PAUSE_DOWNLOAD_SELF_RESOLVED))
                .publish()
                .autoConnect(0);

    }

    @Override
    public Mono<Integer> downloadPiece$(int pieceIndex) {
        return null;
//        System.out.println("start downloading piece: " + pieceIndex);
//        final int REQUEST_BLOCK_SIZE = 16_384;
//        // TODO remove the cast to integer.
//        int pieceLength = this.torrentInfo.getPieceLength(pieceIndex);
//
//        Function<Integer, Integer> requestBlockLength = requestFrom ->
//                Math.min(REQUEST_BLOCK_SIZE, pieceLength - requestFrom);
//
//        Flux<Integer> requestBlockFromPosition = Flux.generate(sink -> {
//            long requestFrom = this.fileSystemLink.getDownloadedBytesInPieces()[pieceIndex];
//            if (requestFrom < pieceLength)
//                // TODO: remove the (int)
//                sink.next((int) requestFrom);
//            else
//                sink.complete();
//        });
//
//        return requestBlockFromPosition.flatMap(requestFromPosition ->
//                        this.peersToPiecesMapper.getPeersWithPiece$(pieceIndex)
//                                .index()
//                                .flatMap(link -> this.allocatorStore.createRequestMessage(link.getT2().getMe(), link.getT2().getPeer(),
//                                        pieceIndex, requestFromPosition, requestBlockLength.apply(requestFromPosition),
//                                        torrentInfo.getPieceLength(pieceIndex))
//                                                .flatMap(requestMessage -> {
//                                                    // System.out.println("trying to download piece: " + pieceIndex + ", begin: " + requestFromPosition + ", from: (" + link.getT1() + ") " + link.getT2().getPeer());
//                                                    return this.blockDownloader.downloadBlock(link.getT2(), requestMessage)
//                                                            .onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty());
//                                                }),
//                                        // concurrency = 1 -> how many peers can I connect to the download the same block concurrently.
//                                        // when the download will end, the stream will be canceled using take(1) operator.
//                                        // prefetch = 1 -> the inner stream is a mono so any number is ok.
//                                        1, 1)
//                                .doOnNext(torrentPieceChanged -> System.out.println("success: " + torrentPieceChanged))
//                                // if I can't download this block in this duration,
//                                // I will skip this piece and try to download the next piece.
//                                .timeout(Duration.ofMillis(20 * 1000))
//                                // make sure to cancel this flux when I downloaded the block
//                                // so I won't try to download the same block from the next peer.
//                                .take(1)
//                // concurrency = 1 -> how many times do I want to download the same block concurrently.
//                // (I may connect to different peer in each try).
//                // prefetch = 1 -> the inner stream is peers Flux so it means that - ***I'm not sure what does prefetch mean here***.
//                , 1, 1)
//                .collectList()
//                .map(torrentPieceChangedList -> pieceIndex)
//                // couldn't download a block from this piece in the specified time.
//                .doOnError(TimeoutException.class, throwable -> System.out.println("stop downloading piece: " + pieceIndex));
    }

    public Flux<TorrentStatusState> getStartDownload$() {
        return startDownload$;
    }

    public Flux<TorrentStatusState> getResumeDownload() {
        return resumeDownload;
    }

    public Flux<TorrentStatusState> getPauseDownload() {
        return pauseDownload;
    }
}
