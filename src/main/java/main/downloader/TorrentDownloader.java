package main.downloader;

import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.file.system.FileSystemLink;
import main.peer.Link;
import main.peer.SearchPeers;
import main.statistics.SpeedStatistics;
import main.torrent.status.TorrentStatusStore;
import reactor.core.publisher.Flux;

public class TorrentDownloader {

    private TorrentInfo torrentInfo;
    private SearchPeers searchPeers;
    private FileSystemLink fileSystemLink;
    private BittorrentAlgorithm bittorrentAlgorithm;
    private TorrentStatusStore torrentStatusStore;
    private SpeedStatistics torrentSpeedStatistics;

    private Flux<Link> peersCommunicatorFlux;

    public TorrentDownloader(TorrentInfo torrentInfo,
                             SearchPeers searchPeers,
                             FileSystemLink fileSystemLink,
                             BittorrentAlgorithm bittorrentAlgorithm,
                             TorrentStatusStore torrentStatusStore,
                             SpeedStatistics torrentSpeedStatistics,
                             Flux<Link> peersCommunicatorFlux) {
        this.torrentInfo = torrentInfo;
        this.searchPeers = searchPeers;
        this.fileSystemLink = fileSystemLink;
        this.bittorrentAlgorithm = bittorrentAlgorithm;
        this.torrentStatusStore = torrentStatusStore;
        this.torrentSpeedStatistics = torrentSpeedStatistics;
        this.peersCommunicatorFlux = peersCommunicatorFlux;
    }

    public SearchPeers getSearchPeers() {
        return searchPeers;
    }

    public BittorrentAlgorithm getBittorrentAlgorithm() {
        return bittorrentAlgorithm;
    }

    public TorrentStatusStore getTorrentStatusStore() {
        return torrentStatusStore;
    }

    public FileSystemLink getFileSystemLink() {
        return fileSystemLink;
    }

    public SpeedStatistics getTorrentSpeedStatistics() {
        return torrentSpeedStatistics;
    }

    public Flux<Link> getPeersCommunicatorFlux() {
        return peersCommunicatorFlux;
    }

    public TorrentInfo getTorrentInfo() {
        return torrentInfo;
    }
}