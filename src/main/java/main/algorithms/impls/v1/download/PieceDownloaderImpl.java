package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PeersToPiecesMapper;
import main.algorithms.PieceDownloader;
import main.file.system.FileSystemLink;
import main.file.system.allocator.AllocatorStore;
import main.peer.Link;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import main.torrent.status.state.tree.TorrentStatusState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import redux.store.Store;

public class PieceDownloaderImpl implements PieceDownloader {

    private TorrentInfo torrentInfo;
    private Store<TorrentStatusState, TorrentStatusAction> store;
    private PeersToPiecesMapper peersToPiecesMapper;
    private FileSystemLink fileSystemLink;
    private BlockDownloader blockDownloader;
    private AllocatorStore allocatorStore;

    public PieceDownloaderImpl(AllocatorStore allocatorStore,
                               TorrentInfo torrentInfo,
                               Store<TorrentStatusState, TorrentStatusAction> store,
                               FileSystemLink fileSystemLink,
                               PeersToPiecesMapper peersToPiecesMapper,
                               BlockDownloader blockDownloader) {
        this.allocatorStore = allocatorStore;
        this.torrentInfo = torrentInfo;
        this.store = store;
        this.peersToPiecesMapper = peersToPiecesMapper;
        this.fileSystemLink = fileSystemLink;
        this.blockDownloader = blockDownloader;
    }

    @Override
    public Mono<Integer> downloadPieceMono(int pieceIndex, Flux<Link> peers$) {
        return null;
    }

    private Flux<RequestMessage> createRequests$(int pieceIndex, Flux<Link> peers$) {
        return Flux.empty();
    }
}
