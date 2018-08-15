package main;

import christophedetroyer.torrent.TorrentParser;
import main.downloader.PieceEvent;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaderBuilder;
import main.downloader.TorrentDownloaders;
import main.file.system.FileSystemLink;
import main.peer.IncomingPeerMessagesNotifier;
import main.peer.Link;
import main.peer.SendMessagesNotifications;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusAction;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.HashSet;

public class App {
    public static Scheduler MyScheduler = Schedulers.elastic();
    private static String downloadPath = System.getProperty("user.dir") + File.separator + "torrents-test" + File.separator;

    private static void f5() throws IOException {
        Mono<TorrentDownloader> torrentDownloader$ = TorrentDownloaderBuilder.buildDefault(getTorrentInfo(), "App", downloadPath)
                .map(TorrentDownloaders.getInstance()::saveTorrentDownloader)
                .cache();

        torrentDownloader$.map(TorrentDownloader::getIncomingPeerMessagesNotifier)
                .flatMapMany(IncomingPeerMessagesNotifier::getIncomingPeerMessages$)
                .map(AbstractMap.SimpleEntry::getValue)
                .subscribe(System.out::println);

        torrentDownloader$.flatMapMany(TorrentDownloader::getPeersCommunicatorFlux)
                .map(Link::sendMessages)
                .flatMap(SendMessagesNotifications::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class)
                .map(requestMessage -> "request: index: " + requestMessage.getIndex() +
                        ", begin: " + requestMessage.getBegin() + ", from: " + requestMessage.getTo())
                .subscribe(System.out::println, Throwable::printStackTrace);

        torrentDownloader$.map(TorrentDownloader::getFileSystemLink)
                .flatMapMany(FileSystemLink::savedBlockFlux)
                .map(PieceEvent::getReceivedPiece)
                .map(pieceMessage -> "received: index: " + pieceMessage.getIndex() +
                        ", begin: " + pieceMessage.getBegin() + ", from: " + pieceMessage.getFrom())
                .subscribe(System.out::println, Throwable::printStackTrace);

        torrentDownloader$.map(TorrentDownloader::getTorrentStatusStore)
                .doOnNext(torrentStatusStore -> torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS))
                .doOnNext(torrentStatusStore -> torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.START_UPLOAD_IN_PROGRESS))
                .doOnNext(torrentStatusStore -> torrentStatusStore.dispatchNonBlocking(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS))
                .subscribe();
    }


    public static void main(String[] args) throws Exception {
        deleteDownloadFolder();
        Hooks.onOperatorDebug();
        f5();
        Thread.sleep(1000 * 1000);
    }

    private static void deleteDownloadFolder() {
        try {
            File file = new File(System.getProperty("user.dir") + File.separator + "torrents-test");
            if (file.exists()) {
                deleteDirectory(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(File directoryToBeDeleted) throws IOException {
        Files.walkFileTree(directoryToBeDeleted.toPath(), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static TorrentInfo getTorrentInfo() throws IOException {
        String torrentFilePath = "src" + File.separator +
                "main" + File.separator +
                "resources" + File.separator +
                "torrents" + File.separator +
                "tor.torrent";
        return new TorrentInfo(torrentFilePath, TorrentParser.parseTorrent(torrentFilePath));
    }
}