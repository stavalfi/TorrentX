package main.file.system;

public interface TorrentFile {
    String getFilePath();

    long getLength();

    long getFrom();

    long getTo();
}
