package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.Downloader;
import main.statistics.SpeedStatistics;

public class TorrentDownloader {

    private TorrentInfo torrentInfo;
    private Downloader downloader;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private DownloadControl downloadControl;
    private SpeedStatistics torrentSpeedStatistics;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             DownloadControl downloadControl,
                             Downloader downloader,
                             SpeedStatistics torrentSpeedStatistics) {
        this.torrentInfo = torrentInfo;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.downloader = downloader;
        this.downloadControl = downloadControl;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public DownloadControl getDownloadControl() {
        return downloadControl;
    }

    public SpeedStatistics getTorrentSpeedStatistics() {
        return torrentSpeedStatistics;
    }

    //    private void algorithm() {
//        this.getPeersCommunicatorFlux()
//                .subscribe(peersCommunicator -> {
//                    peersCommunicator.sendInterestedMessage()
//                            .subscribe(null, null, () -> {
//                                peersCommunicator.getHaveMessageResponseFlux()
//                                        .flatMap(aVoid -> peersCommunicator.getHaveMessageResponseFlux())
//                                        .take(1)
//                                        .flatMap(haveMessage ->
//                                                peersCommunicator.sendRequestMessage(haveMessage.getPieceIndex(), 0, 16000))
//                                        .subscribe();
//                            });
//                });
//    }
}