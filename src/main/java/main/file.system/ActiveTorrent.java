package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class ActiveTorrent extends TorrentInfo {

    private final List<ActiveTorrentFile> activeTorrentFileList;
    private final BitSet bitSet;

    public ActiveTorrent(TorrentInfo torrentInfo, String downloadPath) {
        super(torrentInfo);
        this.bitSet = getPiecesStatus();
        this.activeTorrentFileList = createActiveTorrentFileList(torrentInfo, downloadPath);
    }

    public List<? extends main.file.system.TorrentFile> getTorrentFiles() {
        return this.activeTorrentFileList;
    }

    private BitSet getPiecesStatus() {
        return new BitSet();
    }

    public BitSet getBitSet() {
        return this.bitSet;
    }

    public Mono<ActiveTorrent> writeBlock(PieceMessage pieceMessage) {
        return Mono.empty();
    }

    public synchronized Mono<byte[]> readBlock(RequestMessage requestMessage) {
        return Mono.empty();
    }

    public boolean havePiece(int pieceIndex) {
        throw new NotImplementedException();
    }

    public Mono<ActiveTorrent> updatePieceAsCompleted(int pieceIndex) {
        return Mono.<ActiveTorrent>create(sink -> {
            this.bitSet.set(pieceIndex);
            Mono.just(this);
        }).doOnSuccess(activeTorrent -> {
            // update in mongo db
        });
    }

    private List<ActiveTorrentFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) {
        String mainFolder = downloadPath + "/" + torrentInfo.getName() + "/";

        // create activeTorrentFile list
        long position = 0;
        List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
        for (TorrentFile torrentFile : torrentInfo.getFileList()) {
            String filePath = torrentFile
                    .getFileDirs()
                    .stream()
                    .collect(Collectors.joining("/", mainFolder, ""));
            activeTorrentFileList.add(new ActiveTorrentFile(filePath, position, position + torrentFile.getFileLength()));
            position += torrentFile.getFileLength();
        }
        return activeTorrentFileList;
    }
}
