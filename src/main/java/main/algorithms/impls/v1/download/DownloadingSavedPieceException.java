package main.algorithms.impls.v1.download;

public class DownloadingSavedPieceException extends Exception{
    private int completedPieceIndex;
    public DownloadingSavedPieceException(int completedPieceIndex) {
        this.completedPieceIndex=completedPieceIndex;
    }

    public int getCompletedPieceIndex() {
        return completedPieceIndex;
    }
}
