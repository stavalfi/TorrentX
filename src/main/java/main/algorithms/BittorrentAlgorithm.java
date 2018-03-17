package main.algorithms;

import main.downloader.TorrentPiece;
import reactor.core.publisher.Flux;

public interface BittorrentAlgorithm {
    Flux<TorrentPiece> downloadAsync();
}
