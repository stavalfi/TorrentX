package main.algorithms;

import main.downloader.TorrentPiece;
import main.peer.ReceiveMessages;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<TorrentPiece> downloadAsync();

    ReceiveMessages receiveTorrentMessagesMessagesFlux();
}
