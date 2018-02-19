package main;

import christophedetroyer.torrent.TorrentParser;
import lombok.SneakyThrows;
import main.peer.PeersCommunicator;
import main.peer.PeersProvider;
import main.tracker.TrackerProvider;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

class App {


    private static void f3() {
        String torrentHashCode = splitByPrecentage("1488d454915d860529903b61adb537012a0fe7c8");
        String peerId = splitByPrecentage(HexByteConverter.byteToHex(AppConfig.getInstance().getPeerId().getBytes()));
        String url = "http://torrent.ubuntu.com:6969/announce";

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("info_hash", torrentHashCode);
        vars.put("peer_id", peerId);
        String result = new RestTemplate()
                .getForObject(url + "?info_hash={info_hash}/", String.class, vars);
        System.out.println(result);
    }

    private static String splitByPrecentage(String str) {
        String newStr = "";

        for (int i = 0; i < str.length(); i += 2)
            newStr = newStr.concat(str.charAt(i) + "")
                    .concat(str.charAt(i + 1) + "")
                    .concat("%");
        return newStr.substring(0, newStr.length() - 1);
    }

    private static void f2() throws InterruptedException {

        TorrentInfo torrentInfo = getTorrentInfo();
        System.out.println(torrentInfo);

        TrackerProvider.connectToTrackers(torrentInfo.getTrackerList().stream())
                .flatMap(trackerConnection -> PeersProvider.connectToPeers(trackerConnection, torrentInfo.getTorrentInfoHash()))
                .doOnNext(peersCommunicator -> sendHave(torrentInfo, peersCommunicator))
                .doOnNext(peersCommunicator ->
                        peersCommunicator.sendRequestMessage(0, 0, 1000).block())
                .flatMap(peersCommunicator -> peersCommunicator.receive().subscribeOn(Schedulers.elastic()))
                .subscribe(System.out::println, System.out::println);

        Thread.sleep(1000 * 1000);
    }

    private static void sendHave(TorrentInfo torrentInfo, PeersCommunicator peersCommunicator) {
        Flux.fromStream(IntStream.range(1, torrentInfo.getPieces().size()).boxed())
                .flatMap(peersCommunicator::sendHaveMessage)
                .collectList()
                .block();
    }

    public static void main(String[] args) throws Exception {
        Hooks.onErrorDropped(throwable -> System.out.println("exception thrown after a stream terminated: " + throwable));
        f3();
    }

    @SneakyThrows
    public static TorrentInfo getTorrentInfo() {
        String torrentFilePath = "src/main/resources/torrent-file-example.torrent";
        return new TorrentInfo(TorrentParser.parseTorrent(torrentFilePath));
    }
}

//  Main tracker: http://torrent.ubuntu.com:6969/announce
//  Comment: Ubuntu CD releases.ubuntu.com
//  Info_hash: 1488d454915d860529903b61adb537012a0fe7c8
//  Name: ubuntu-16.04.3-desktop-amd64.iso
//  Piece Length: 524288
//  Pieces: 3029
//  Total Size: 1587609600
//  Is Single File Torrent: true
//  File List:
//  Tracker List:
//  http://torrent.ubuntu.com:6969/announce
//  http://ipv6.torrent.ubuntu.com:6969/announce


//    torrent hash: af1f3dbc5d5baeaf83f812e06aa91bbd7b55cce8
//    udp://tracker.coppersurfer.tk:6969/scrape
//    udp://9.rarbg.com:2710/scrape
//    udp://p4p.arenabg.com:1337
//    udp://tracker.leechers-paradise.org:6969
//    udp://tracker.internetwarriors.net:1337
//    udp://tracker.opentrackr.org:1337/scrape

//    Peer(ipAddress=35.160.55.32, tcpPort=8104)
//    Peer(ipAddress=52.59.116.245, tcpPort=8114)
//    Peer(ipAddress=68.45.123.113, tcpPort=6881)
//    Peer(ipAddress=84.76.9.211, tcpPort=6881)
//    Peer(ipAddress=86.184.180.210, tcpPort=6881)
//    Peer(ipAddress=100.11.128.230, tcpPort=6881)
//    Peer(ipAddress=138.36.166.98, tcpPort=6881)
//    Peer(ipAddress=189.61.65.16, tcpPort=6881)
//    Peer(ipAddress=200.84.37.8, tcpPort=23384)
//    Peer(ipAddress=192.142.208.111, tcpPort=10373)
//    Peer(ipAddress=184.66.152.106, tcpPort=60687)
//    Peer(ipAddress=178.221.238.96, tcpPort=18313)
//    Peer(ipAddress=141.134.130.15, tcpPort=21011)
//    Peer(ipAddress=109.122.123.158, tcpPort=36658)
//    Peer(ipAddress=109.76.175.237, tcpPort=33967)
//    Peer(ipAddress=105.110.217.255, tcpPort=53719)
//    Peer(ipAddress=88.101.57.60, tcpPort=43681)
//    Peer(ipAddress=86.170.167.230, tcpPort=46033)
//    Peer(ipAddress=82.212.112.193, tcpPort=35885)
//    Peer(ipAddress=79.169.249.147, tcpPort=15373)
//    Peer(ipAddress=76.11.44.90, tcpPort=58659)
//    Peer(ipAddress=72.11.163.214, tcpPort=30055)
//    Peer(ipAddress=64.119.192.60, tcpPort=6881)
//    Peer(ipAddress=51.36.120.250, tcpPort=44007)
//    Peer(ipAddress=37.216.23.43, tcpPort=45254)
//    Peer(ipAddress=5.29.96.159, tcpPort=8091)
//    Peer(ipAddress=200.84.37.8, tcpPort=23384)
//    Peer(ipAddress=5.29.96.159, tcpPort=8091)
//    Peer(ipAddress=34.242.175.64, tcpPort=8102)
//    Peer(ipAddress=35.160.55.32, tcpPort=8104)
//    Peer(ipAddress=52.59.116.245, tcpPort=8114)
//    Peer(ipAddress=68.45.123.113, tcpPort=6881)
//    Peer(ipAddress=86.184.180.210, tcpPort=6881)
//    Peer(ipAddress=100.11.128.230, tcpPort=6881)
//    Peer(ipAddress=189.61.65.16, tcpPort=6881)
//    Peer(ipAddress=200.84.37.8, tcpPort=23384)
//    Peer(ipAddress=197.15.22.165, tcpPort=30278)
//    Peer(ipAddress=190.231.216.227, tcpPort=12365)
//    Peer(ipAddress=190.207.55.62, tcpPort=55151)
//    Peer(ipAddress=190.19.100.142, tcpPort=35289)
//    Peer(ipAddress=186.179.101.178, tcpPort=55252)
//    Peer(ipAddress=181.121.58.219, tcpPort=43274)
//    Peer(ipAddress=178.221.238.96, tcpPort=18313)
//    Peer(ipAddress=160.0.192.16, tcpPort=28382)
//    Peer(ipAddress=141.134.130.15, tcpPort=21011)
//    Peer(ipAddress=139.47.31.207, tcpPort=51413)
//    Peer(ipAddress=109.122.123.158, tcpPort=36658)
//    Peer(ipAddress=109.76.175.237, tcpPort=33967)
//    Peer(ipAddress=105.110.217.255, tcpPort=53719)
//    Peer(ipAddress=103.248.15.2, tcpPort=64823)
//    Peer(ipAddress=95.120.146.30, tcpPort=45682)
//    Peer(ipAddress=88.101.57.60, tcpPort=43681)
//    Peer(ipAddress=87.219.127.196, tcpPort=53497)
//    Peer(ipAddress=86.170.167.230, tcpPort=46033)
//    Peer(ipAddress=85.58.149.165, tcpPort=37096)
//    Peer(ipAddress=83.165.13.182, tcpPort=16471)
//    Peer(ipAddress=83.58.141.54, tcpPort=61137)
//    Peer(ipAddress=82.159.186.34, tcpPort=50514)
//    Peer(ipAddress=79.169.249.147, tcpPort=15373)
//    Peer(ipAddress=76.11.44.90, tcpPort=58659)
//    Peer(ipAddress=72.11.163.214, tcpPort=30055)
//    Peer(ipAddress=51.36.120.250, tcpPort=44007)
//    Peer(ipAddress=39.33.67.18, tcpPort=30388)
//    Peer(ipAddress=37.228.255.218, tcpPort=13817)
//    Peer(ipAddress=37.216.23.43, tcpPort=45254)
//    Peer(ipAddress=24.89.206.212, tcpPort=21346)
//    Peer(ipAddress=5.29.96.159, tcpPort=8091)
//    Peer(ipAddress=2.138.213.235, tcpPort=56095)
//    Peer(ipAddress=200.84.37.8, tcpPort=23384)
//    Peer(ipAddress=5.29.96.159, tcpPort=8091)
//    Peer(ipAddress=35.160.55.32, tcpPort=8104)
//    Peer(ipAddress=52.59.116.245, tcpPort=8114)
//    Peer(ipAddress=84.76.9.211, tcpPort=6881)
//    Peer(ipAddress=138.36.166.98, tcpPort=6881)
//    Peer(ipAddress=200.84.37.8, tcpPort=23384)
//    Peer(ipAddress=192.142.208.111, tcpPort=10373)
//    Peer(ipAddress=82.212.112.193, tcpPort=35885)
//    Peer(ipAddress=5.29.96.159, tcpPort=8091)
