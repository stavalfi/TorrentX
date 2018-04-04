package main.algorithms;

import main.downloader.TorrentPieceChanged;
import main.peer.ReceiveMessages;
import reactor.core.publisher.ConnectableFlux;

public interface BittorrentAlgorithm {
    ConnectableFlux<TorrentPieceChanged> startDownloadFlux();

    ReceiveMessages receiveTorrentMessagesMessagesFlux();
}
