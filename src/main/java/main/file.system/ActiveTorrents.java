package main.file.system;

import main.TorrentInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class ActiveTorrents {

    private static ActiveTorrents instance = new ActiveTorrents();

    public static ActiveTorrents getInstance() {
        return instance;
    }

    public Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath) {
        new ActiveTorrent(torrentInfo, downloadPath, null, null);
        return Mono.never();
    }

    public Mono<ActiveTorrent> deleteActiveTorrentOnlyMono(TorrentInfo torrentInfo, String downloadPath) {
        new ActiveTorrent(torrentInfo, downloadPath, null, null);
        return Mono.never();
    }

    public Mono<ActiveTorrent> deleteActiveTorrentAndFileMono(TorrentInfo torrentInfo, String downloadPath) {
        new ActiveTorrent(torrentInfo, downloadPath, null, null);
        return Mono.never();
    }

    public Mono<ActiveTorrent> deleteFileOnlyMono(TorrentInfo torrentInfo, String filePath) {
        new ActiveTorrent(torrentInfo, filePath, null, null);
        return Mono.never();
    }

    public Flux<ActiveTorrent> getAllActiveTorrentsFlux() {
        return Flux.never();
    }


    public Mono<ActiveTorrent> getActiveTorrentByHashMono(String torrentInfoHash) {
        return Mono.never();
    }


    public class ActiveTorrent extends TorrentInfo {
        private final String downloadPath;
        private final RandomAccessFile randomAccessFile;
        private final BitSet bitSet;

        private ActiveTorrent(TorrentInfo torrentInfo, String downloadPath, RandomAccessFile randomAccessFile, BitSet bitSet) {
            super(torrentInfo);
            this.downloadPath = downloadPath;
            this.randomAccessFile = randomAccessFile;
            this.bitSet = bitSet;
        }

        public BitSet getBitSet() {
            return this.bitSet;
        }

        public Mono<ActiveTorrent> saveBlock(int pieceIndex, int offset, byte[] block) {
            throw new NotImplementedException();
        }

        public Mono<ByteBuffer> readBlock(int pieceIndex, int offset) {
            if (!havePiece(pieceIndex))
                Mono.error(new IllegalStateException("requested block of pieced we don't have yet: " + pieceIndex));
            return Mono.never();
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

        public String getDownloadPath() {
            return downloadPath;
        }
    }
}
