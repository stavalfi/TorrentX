package main.file.system;

import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ActiveTorrent extends TorrentInfo {

    private final List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
    private final BitSet bitSet;

    private ActiveTorrent(TorrentInfo torrentInfo, BitSet bitSet) {
        super(torrentInfo);
        this.bitSet = bitSet;
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
}
