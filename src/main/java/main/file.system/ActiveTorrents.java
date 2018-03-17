package main.file.system;

import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Optional;

public class ActiveTorrents {

    private static ActiveTorrents instance = new ActiveTorrents();

    public static ActiveTorrents getInstance() {
        return instance;
    }

    public Mono<ActiveTorrent> createActiveTorrentMono(TorrentInfo torrentInfo, String downloadPath) {
        return Mono.never();
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentOnlyMono(String torrentInfoHash) {
        return Mono.never();
    }

    public Mono<Optional<ActiveTorrent>> deleteActiveTorrentAndFileMono(String torrentInfoHash) {
        return Mono.never();
    }

    public Mono<Optional<ActiveTorrent>> deleteFileOnlyMono(String torrentInfoHash) {
        return Mono.never();
    }

    public Mono<Optional<ActiveTorrent>> findActiveTorrentByHashMono(String torrentInfoHash) {
        return Mono.never();
    }

    public Flux<ActiveTorrent> getAllActiveTorrentsFlux() {
        return Flux.never();
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

        // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
        // something can go wrong if multiple threads try to read/write concurrently.
        public synchronized Mono<ActiveTorrent> writeBlock(PieceMessage pieceMessage) {
            throw new NotImplementedException();
        }


        // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
        // something can go wrong if multiple threads try to read/write concurrently.
        public synchronized Mono<byte[]> readBlock(RequestMessage requestMessage) {
            //if (!havePiece(pieceIndex))
            //Mono.error(new IllegalStateException("requested block of pieced we don't have yet: " + pieceIndex));
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
