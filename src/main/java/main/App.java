package main;

import christophedetroyer.torrent.TorrentParser;
import main.downloader.PieceEvent;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.peer.Link;
import main.peer.SendMessagesNotifications;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.StatusType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class App {
    public static Scheduler MyScheduler = Schedulers.elastic();
    private static String downloadPath = System.getProperty("user.dir") + File.separator + "torrents-test" + File.separator;


    public static void f5() throws IOException, InterruptedException {
        System.out.println(getTorrentInfo());
    }

    private static SeekableByteChannel createFile(String filePathToCreate) throws IOException {
        OpenOption[] options = {
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SPARSE,
                StandardOpenOption.READ
                // TODO: think if we add CREATE if exist rule.
        };
        SeekableByteChannel seekableByteChannel = Files.newByteChannel(Paths.get(filePathToCreate), options);
        ByteBuffer allocate = ByteBuffer.allocate(4).putInt(1);
        allocate.rewind();
        int write = seekableByteChannel.write(allocate);
        assert write == 4;
        return seekableByteChannel;
    }

    private static void f4() throws IOException {
        TorrentDownloader torrentDownloader = TorrentDownloaders
                .createDefaultTorrentDownloader(getTorrentInfo(), downloadPath);

        torrentDownloader.getPeersCommunicatorFlux()
                .map(Link::sendMessages)
                .flatMap(SendMessagesNotifications::sentPeerMessagesFlux)
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class)
                .map(requestMessage -> "request: index: " + requestMessage.getIndex() +
                        ", begin: " + requestMessage.getBegin() + ", from: " + requestMessage.getTo())
                .subscribe(System.out::println, Throwable::printStackTrace);

        torrentDownloader.getFileSystemLink()
                .savedBlockFlux()
                .map(PieceEvent::getReceivedPiece)
                .map(pieceMessage -> "received: index: " + pieceMessage.getIndex() +
                        ", begin: " + pieceMessage.getBegin() + ", from: " + pieceMessage.getFrom())
                .subscribe(System.out::println, Throwable::printStackTrace);

        torrentDownloader.getStatusChanger()
                .changeState(StatusType.START_DOWNLOAD)
                .publishOn(Schedulers.elastic())
                .block();
        torrentDownloader.getStatusChanger()
                .changeState(StatusType.START_UPLOAD)
                .publishOn(Schedulers.elastic())
                .block();
    }

    public static void main(String[] args) throws Exception {
        //Hooks.onOperatorDebug();
        f5();
        Thread.sleep(1000 * 1000);
    }

    private static TorrentInfo getTorrentInfo() throws IOException {
        String torrentFilePath = "src" + File.separator +
                "main" + File.separator +
                "resources" + File.separator +
                "torrents" + File.separator +
                "torrent-file-example1.torrent";
        return new TorrentInfo(torrentFilePath, TorrentParser.parseTorrent(torrentFilePath));
    }
}