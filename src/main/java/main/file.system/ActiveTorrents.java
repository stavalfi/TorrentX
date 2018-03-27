package main.file.system;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveTorrents {

    private CopyOnWriteArrayList<ActiveTorrent> activeTorrentList = new CopyOnWriteArrayList<>();

    public Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath) {
        // TODO: check if this torrent exist in db.
        return Mono.<ActiveTorrent>just(new ActiveTorrent(torrentInfo, downloadPath))
                .subscribeOn(Schedulers.elastic())
                .doOnNext(activeTorrent -> {
                    // save this torrent information in db.
                    activeTorrentList.add(activeTorrent);
                }).flatMap(activeTorrent -> createFolders(torrentInfo, downloadPath)
                        .flatMap(activeTorrents -> createFiles(torrentInfo, downloadPath))
                        .map(activeTorrents -> activeTorrent));
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentAndFilesMono(String torrentInfoHash) {
        // delete activeTorrent from list
        return deleteActiveTorrentOnlyMono(torrentInfoHash)
                .flatMap(activeTorrent -> deleteFileOnlyMono(torrentInfoHash));
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentOnlyMono(String torrentInfoHash) {
        return findActiveTorrentByHashMono(torrentInfoHash)
                .filter(Optional::isPresent)
                .flatMap(activeTorrent -> {
                    this.activeTorrentList = this.activeTorrentList.stream()
                            .filter(activeTorrent1 -> !activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash))
                            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    return Mono.just(activeTorrent);
                }).subscribeOn(Schedulers.elastic());
    }

    public Mono<Optional<ActiveTorrent>> deleteFileOnlyMono(String torrentInfoHash) {
        return findActiveTorrentByHashMono(torrentInfoHash)
                .filter(Optional::isPresent)
                .doOnNext(activeTorrent -> activeTorrent.get()
                        .getTorrentFiles()
                        .stream()
                        .map(TorrentFile::getFilePath)
                        .map(File::new)
                        .forEach(this::completelyDeleteFolder));
    }

    private boolean completelyDeleteFolder(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                completelyDeleteFolder(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public Mono<Optional<ActiveTorrent>> findActiveTorrentByHashMono(String torrentInfoHash) {
        Optional<ActiveTorrent> first = this.activeTorrentList.stream()
                .filter(activeTorrent -> activeTorrent.getTorrentInfoHash().equals(torrentInfoHash))
                .findFirst();
        return Mono.just(first);
    }

    public Flux<ActiveTorrent> getActiveTorrentsFlux() {
        // TODO: get and load all torrents from db.
        return Flux.fromIterable(this.activeTorrentList);
    }

    private static ActiveTorrents instance = new ActiveTorrents();

    public static ActiveTorrents getInstance() {
        return instance;
    }

    private Mono<ActiveTorrents> createFolders(TorrentInfo torrentInfo, String downloadPath) {
        // create main folder for the download of the torrent.
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + "/" + torrentInfo.getName() + "/" :
                downloadPath + "/";
        return Mono.create(sink -> {
            createFolder(mainFolder);

            // create sub folders for the download of the torrent
            torrentInfo.getFileList()
                    .stream()
                    .map(christophedetroyer.torrent.TorrentFile::getFileDirs)
                    .filter(folders -> folders.size() > 1)
                    .map(folders -> folders.subList(0, folders.size() - 2))
                    .map(List::stream)
                    .map(stringStream -> stringStream.collect(Collectors.joining("/", mainFolder, "")))
                    .forEach(folderPath -> createFolder(folderPath));
            sink.success(this);
        });
    }

    private Mono<ActiveTorrents> createFiles(TorrentInfo torrentInfo, String downloadPath) {
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + "/" + torrentInfo.getName() + "/" :
                downloadPath + "/";
        // create files in each folder.
        return Mono.<ActiveTorrents>create(sink -> {
            for (christophedetroyer.torrent.TorrentFile torrentFile : torrentInfo.getFileList()) {
                String filePath = torrentFile
                        .getFileDirs()
                        .stream()
                        .collect(Collectors.joining("/", mainFolder, ""));
                try {
                    createFile(filePath, torrentFile.getFileLength());
                } catch (IOException e) {
                    sink.error(e);
                    return;
                }
            }
            sink.success(this);
        }).doOnError(throwable -> completelyDeleteFolder(new File(mainFolder)));
    }

    private void createFolder(String path) {
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.mkdirs();
    }

    private void createFile(String filePathToCreate, long length) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePathToCreate, "rw");
        randomAccessFile.setLength(length);
    }

}

