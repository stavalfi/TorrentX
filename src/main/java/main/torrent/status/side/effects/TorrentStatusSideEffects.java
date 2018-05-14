package main.torrent.status.side.effects;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.peer.PeersListener;
import main.torrent.status.StatusNotifier;

public class TorrentStatusSideEffects {
    private TorrentInfo torrentInfo;
    private StatusNotifier statusNotifier;
    private PeersListener peersListener;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;

    // TODO: uncomment
//    private Flux<Status> torrentRemovedEffect$ = getChangeReason()
//            .filter(statusType -> statusType.equals(Action.REMOVE_TORRENT))
//            .flatMap(statusType -> this.peersListener.pauseListenToIncomingPeers())
//            // TODO: we need to stop search for new peers.
//            .publish()
//            .autoConnect(0);
//
//    private Flux<Status> filesRemovedEffect$ = getChangeReason()
//            .filter(statusType -> statusType.equals(Action.REMOVE_FILES))
//            .flatMap(statusType -> this.peersListener.pauseListenToIncomingPeers())
//            .publish()
//            .autoConnect(0);


    public TorrentStatusSideEffects(TorrentInfo torrentInfo,
                                    StatusNotifier statusNotifier,
                                    PeersListener peersListener,
                                    FileSystemLink fileSystemLink,
                                    BittorrentAlgorithm bittorrentAlgorithm) {
        this.torrentInfo = torrentInfo;
        this.statusNotifier = statusNotifier;
        this.peersListener = peersListener;
        this.fileSystemLink = fileSystemLink;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
    }

//    private Flux<Action> getChangeReason() {
//        return this.statusNotifier
//                .getState$()
//                .map(Status::getChangeReason)
//                .filter(Optional::isPresent)
//                .map(Optional::get);
//    }
}
