package main.file;

import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class MyTorrents {

    private static MyTorrents instance = new MyTorrents();

    public static MyTorrents getInstance() {
        return instance;
    }

    // TODO: change to concurrent list.
    private List<ActiveTorrent> files = new LinkedList<>();

    public Stream<ActiveTorrent> getFiles() {
        return files.stream();
    }

    public Mono<ActiveTorrent> createFile(ActiveTorrent activeTorrent) {
        return Mono.never();
    }
}
