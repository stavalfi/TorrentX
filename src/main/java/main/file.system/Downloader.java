package main.file.system;

import main.downloader.TorrentPieceChanged;
import reactor.core.publisher.Flux;

public interface Downloader {
    Flux<TorrentPieceChanged> downloadAsync();
}
