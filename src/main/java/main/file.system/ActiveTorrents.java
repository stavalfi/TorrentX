package main.file.system;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.RandomAccessFile;
import java.util.Optional;

public class ActiveTorrents {

    private static ActiveTorrents instance = new ActiveTorrents();

    public static ActiveTorrents getInstance() {
        return instance;
    }

    public Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath) {
        //Mono.just(new ActiveTorrent(torrentInfo, downloadPath, new RandomAccessFile()))

        return Mono.empty();
    }

    private Mono<RandomAccessFile> createFolder(TorrentInfo torrentInfo, String downloadPath) {
        return Mono.<RandomAccessFile>create(sink -> {
//            Path dir = Paths.get(downloadPath + "/" + torrentInfo.getName());
//            try {
//                Files.createDirectory(dir);
//
//                RandomAccessFile randomAccessFile = new RandomAccessFile(downloadPath+"/.txt", "rw");
//                randomAccessFile.setLength(1024 * 1024);
//                sink.success(randomAccessFile);
//            } catch (IOException e) {
//                sink.error(new IOException(e));
//                return;
//            }
        });
    }

//    private static Mono<RandomAccessFile> createFolder(TorrentInfo torrentInfo, String downloadPath) {
//        return Mono.<RandomAccessFile>create(sink -> {
//            Path dir = Paths.get(downloadPath + "/" + torrentInfo.getName());
//            try {
//                Files.createDirectory(dir);
//
//                RandomAccessFile randomAccessFile = new RandomAccessFile(downloadPath+"/.txt", "rw");
//                randomAccessFile.setLength(1024 * 1024);
//                sink.success(randomAccessFile);
//            } catch (IOException e) {
//                sink.error(new IOException(e));
//                return;
//            }
//        });
//    }








    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentOnlyMono(String torrentInfoHash) {
        return Mono.empty();
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentAndFileMono(String torrentInfoHash) {
        return Mono.empty();
    }

    public Mono<Optional<ActiveTorrent>> deleteFileOnlyMono(String torrentInfoHash) {
        return Mono.empty();
    }

    public Mono<Optional<ActiveTorrent>> findActiveTorrentByHashMono(String torrentInfoHash) {
        return Mono.never();
    }

    public Flux<ActiveTorrent> getAllActiveTorrentsFlux() {
        return Flux.never();
    }
}

