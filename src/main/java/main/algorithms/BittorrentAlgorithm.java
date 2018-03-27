package main.algorithms;

import main.downloader.TorrentPieceChanged;
import main.peer.ReceiveMessages;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<TorrentPieceChanged> downloadAsync();

    ReceiveMessages receiveTorrentMessagesMessagesFlux();
}
