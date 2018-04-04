package main.file.system;

import main.App;
import main.TorrentInfo;
import main.torrent.status.TorrentStatus;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ActiveTorrents {

    private CopyOnWriteArrayList<ActiveTorrent> activeTorrentList = new CopyOnWriteArrayList<>();

    public Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath,
                                                       TorrentStatus torrentStatus,
                                                       Flux<PieceMessage> peerResponsesFlux) {

        torrentStatus.getStatusTypeFlux()
                .flatMap(torrentStatusType -> {
                    switch (torrentStatusType) {
                        case REMOVE_FILES:
                            return deleteFileOnlyMono(torrentInfo.getTorrentInfoHash());
                        case REMOVE_TORRENT:
                            return deleteActiveTorrentOnlyMono(torrentInfo.getTorrentInfoHash());
                        default:
                            return Flux.empty();
                    }
                }).subscribe();

        // TODO: check if this torrent exist in db.
        Mono<ActiveTorrent> createActiveTorrentMono = createFolders(torrentInfo, downloadPath)
                .flatMap(activeTorrents -> createFiles(torrentInfo, downloadPath))
                .map(activeTorrents -> new ActiveTorrent(torrentInfo, downloadPath,
                        torrentStatus, peerResponsesFlux))
                .doOnNext(activeTorrent ->
                        // save this torrent information in db.
                        this.activeTorrentList.add(activeTorrent));

        // firstly, check if there is an active-torrent exist already.
        // if yes, return it, else create one using the above Mono: "createActiveTorrentMono"
        return findActiveTorrentByHashMono(torrentInfo.getTorrentInfoHash())
                .flatMap(activeTorrentOptional -> {
                    if (activeTorrentOptional.isPresent())
                        return Mono.just(activeTorrentOptional.get());
                    else
                        return createActiveTorrentMono;
                });
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentOnlyMono(String torrentInfoHash) {
        return findActiveTorrentByHashMono(torrentInfoHash)
                .filter(Optional::isPresent)
                .flatMap(activeTorrent -> {
                    this.activeTorrentList = this.activeTorrentList.stream()
                            .filter(activeTorrent1 -> !activeTorrent1.getTorrentInfoHash().equals(torrentInfoHash))
                            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    return Mono.just(activeTorrent);
                }).subscribeOn(App.MyScheduler);
    }

    public Mono<Optional<ActiveTorrent>> deleteFileOnlyMono(String torrentInfoHash) {
        return findActiveTorrentByHashMono(torrentInfoHash)
                .filter(Optional::isPresent)
                .doOnNext(activeTorrent -> {
                    activeTorrent.get()
                            .getTorrentFiles()
                            .stream()
                            .map(TorrentFile::getFilePath)
                            .map(File::new)
                            .forEach(this::completelyDeleteFolder);
                    String filePath = activeTorrent.get().getDownloadPath() + "/" + activeTorrent.get().getName();
                    File mainFile = new File(filePath);
                    completelyDeleteFolder(mainFile);
                });
    }

    public Mono<Optional<ActiveTorrent>> findActiveTorrentByHashMono(String torrentInfoHash) {
        Optional<ActiveTorrent> first = this.activeTorrentList.stream()
                .filter(activeTorrent -> activeTorrent.getTorrentInfoHash().equals(torrentInfoHash))
                .findFirst();
        return Mono.just(first);
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
                downloadPath + torrentInfo.getName() + "/" :
                downloadPath;
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
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        file.mkdirs();
    }

    private void createFile(String filePathToCreate, long length) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(filePathToCreate, "rw");
        randomAccessFile.setLength(length);
    }

}

