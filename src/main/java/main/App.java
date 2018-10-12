package main;

import christophedetroyer.torrent.TorrentParser;
import main.download.manager.TorrentDownloader;
import main.download.manager.TorrentDownloaderBuilder;
import main.download.manager.TorrentDownloaders;
import main.peer.Link;
import main.peer.algorithms.SendMessagesNotifications;
import main.peer.peerMessages.HaveMessage;
import main.peers.listener.ListenerAction;
import main.torrent.status.TorrentStatusAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class App {
    public static Scheduler timeoutScheduler = Schedulers.newSingle("TIMEOUT");
    public static Scheduler timeoutFallbackScheduler = Schedulers.newSingle("TIMEOUT-FALLBACK");
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static String downloadPath = System.getProperty("user.dir") + File.separator + "torrents-test" + File.separator;

    private static void f5() throws IOException, InterruptedException {
        TorrentDownloader torrentDownloader = TorrentDownloaderBuilder.buildDefault(getTorrentInfo(), "App", downloadPath);

        torrentDownloader.getFileSystemLink()
                .savedPieces$()
                .map(completedPieceIndex ->
                        IntStream.range(0, torrentDownloader.getTorrentInfo().getPieces().size())
                                .mapToObj(pieceIndex -> pieceIndex == completedPieceIndex ? "*" : torrentDownloader.getFileSystemLink().havePiece(pieceIndex) ? "1" : "0")
                                .collect(Collectors.joining()))
                .map(str -> str.substring(0, 140))
                .subscribe(System.out::println);

//        torrentDownloader.getIncomingPeerMessagesNotifier()
//                .getPieceMessageResponseFlux()
//                .subscribe(System.out::println);

        torrentDownloader.getPeersCommunicatorFlux()
                .map(Link::sendMessages)
                .flatMap(SendMessagesNotifications::sentPeerMessages$)
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class)
                .map(haveMessage -> "sent: " + haveMessage.toString())
                .subscribe(System.out::println);

        TorrentDownloaders.getListenStore().dispatchNonBlocking(ListenerAction.START_LISTENING_IN_PROGRESS);
        torrentDownloader.getTorrentStatusStore().dispatchNonBlocking(TorrentStatusAction.START_DOWNLOAD_IN_PROGRESS);
        torrentDownloader.getTorrentStatusStore().dispatchNonBlocking(TorrentStatusAction.START_UPLOAD_IN_PROGRESS);
        torrentDownloader.getTorrentStatusStore().dispatchNonBlocking(TorrentStatusAction.START_SEARCHING_PEERS_IN_PROGRESS);
    }


    public static void main(String[] args) throws Exception {
        deleteDownloadFolder();
        Hooks.onOperatorDebug();
        f5();
        Thread.sleep(10000 * 1000);
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
                "80mb-20peers.torrent";
        TorrentInfo torrentInfo = new TorrentInfo(torrentFilePath, TorrentParser.parseTorrent(torrentFilePath));
        System.out.println(torrentInfo);
        System.out.println("--------------------------------------");
        System.out.println("--------------------------------------");
        System.out.println("--------------------------------------");
        return torrentInfo;
    }
}