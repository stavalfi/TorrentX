package main.algorithms;

public class DownloadAlgorithm {
    private PiecesDownloader piecesDownloader;
    private BlockDownloader blockDownloader;
    private PeersToPiecesMapper peersToPiecesMapper;

    public DownloadAlgorithm(PiecesDownloader piecesDownloader,
                             BlockDownloader blockDownloader,
                             PeersToPiecesMapper peersToPiecesMapper) {
        this.piecesDownloader = piecesDownloader;
        this.blockDownloader = blockDownloader;
        this.peersToPiecesMapper = peersToPiecesMapper;
    }

    public PiecesDownloader getPiecesDownloader() {
        return piecesDownloader;
    }

    public BlockDownloader getBlockDownloader() {
        return blockDownloader;
    }

    public PeersToPiecesMapper getPeersToPiecesMapper() {
        return peersToPiecesMapper;
    }
}
