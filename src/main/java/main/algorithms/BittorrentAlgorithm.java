package main.algorithms;

public class BittorrentAlgorithm {
    private UploadAlgorithm uploadAlgorithm;
    private DownloadAlgorithm downloadAlgorithm;
    private NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm;

    public BittorrentAlgorithm(UploadAlgorithm uploadAlgorithm,
                               DownloadAlgorithm downloadAlgorithm,
                               NotifyAboutCompletedPieceAlgorithm notifyAboutCompletedPieceAlgorithm) {
        this.uploadAlgorithm = uploadAlgorithm;
        this.downloadAlgorithm = downloadAlgorithm;
        this.notifyAboutCompletedPieceAlgorithm = notifyAboutCompletedPieceAlgorithm;
    }

    public UploadAlgorithm getUploadAlgorithm() {
        return uploadAlgorithm;
    }

    public DownloadAlgorithm getDownloadAlgorithm() {
        return downloadAlgorithm;
    }

    public NotifyAboutCompletedPieceAlgorithm getNotifyAboutCompletedPieceAlgorithm() {
        return notifyAboutCompletedPieceAlgorithm;
    }
}
