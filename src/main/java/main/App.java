package main;

import christophedetroyer.torrent.TorrentParser;
import lombok.SneakyThrows;
import main.tracker.TrackerProvider;

class App {


    private static void f4() {

        TorrentInfo torrentInfo = getTorrentInfo();
        new TrackerProvider(torrentInfo)
                .connectToTrackersFlux()
                .autoConnect()
                .limitRequest(1)
                .subscribe(System.out::println, Throwable::printStackTrace, () -> System.out.println("finish!!!!"));
//
//        ActiveTorrent activeTorrent = new ActiveTorrent(torrentInfo, null);
//        Downloader downloader = new Downloader(activeTorrent);
//        TorrentDownloader torrentDownloader = new TorrentDownloaderImpl(torrentInfo, downloader);
//
//        torrentDownloader.getPieceMessageResponseFlux()
//                .subscribe(System.out::println, System.out::println, System.out::println);
//
//        torrentDownloader.start();
    }

    public static void main(String[] args) throws Exception {
        f4();
        Thread.sleep(1000 * 1000);
    }

    @SneakyThrows
    public static TorrentInfo getTorrentInfo() {
        String torrentFilePath = "src/main/resources/torrent-file-example3.torrent";
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
//    udp://tracker.coppersurfer.tk:6969/scrapeMono
//    udp://9.rarbg.com:2710/scrapeMono
//    udp://p4p.arenabg.com:1337
//    udp://tracker.leechers-paradise.org:6969
//    udp://tracker.internetwarriors.net:1337
//    udp://tracker.opentrackr.org:1337/scrapeMono

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

//    private static void f3() throws MalformedURLException {
//
//
//        System.out.println(HexByteConverter.byteToHex(AppConfig.getInstance().getPeerId().getBytes()));
//        TorrentInfo torrentInfo = getTorrentInfo();
//        System.out.println(torrentInfo);
//        String torrentHashCode = splitByPrecentage("1488d454915d860529903b61adb537012a0fe7c8");
//        String peerId = splitByPrecentage(HexByteConverter.byteToHex(AppConfig.getInstance().getPeerId().getBytes()));
//        System.out.println(torrentHashCode);
//        System.out.println(peerId);
//        Map<String, String> vars = new HashMap<String, String>();
//        vars.put("info_hash", torrentHashCode);
//        vars.put("peer_id", peerId);
//
//        String url = "http://torrent.ubuntu.com:6969/announce?info_hash=%14%88%d4%54%91%5d%86%05%29%90%3b%61%ad%b5%37%01%2a%0f%e7%c8&peer_id=%2D%41%5A%35%37%35%30%2D%54%70%6B%58%74%74%5A%4C%66%70%53%48";
//
//        String result = new RestTemplate()
//                .getForObject(url, String.class);
//        System.out.println(result);
//    }
//
//    private static String splitByPrecentage(String str) {
//        String newStr = "";
//
//        for (int i = 0; i < str.length(); i += 2)
//            newStr = newStr.concat("%").concat(str.charAt(i) + "")
//                    .concat(str.charAt(i + 1) + "");
//
//        return newStr;
//    }

//    final CountDownLatch latch = new CountDownLatch(2);
//    TcpServer server = TcpServer.create(8091);
//    ObjectMapper m = new ObjectMapper();
//
//        server.newHandler((in, out) -> {
//                in.receive()
//                .asByteArray()
//                .map(bb -> {
//                try {
//                return m.readValue(bb, Person.class);
//        } catch (IOException io) {
//        throw Exceptions.propagate(io);
//        }
//        })
//        .subscribe(data -> {
//        System.out.println("server got something!!: " + data);
//        });
//
//        return out.sendString(Mono.just("Hi"))
//        .neverComplete();
//        }).subscribe();
//
//final TcpClient client = TcpClient.create(opts -> opts.host("localhost").port(8091));
//
//        client.newHandler((in, out) -> {
//        //in
//        in.receive()
//        .asByteArray()
//        .map(bb -> {
//        try {
//        return m.readValue(bb, String.class);
//        } catch (IOException io) {
//        throw Exceptions.propagate(io);
//        }
//        })
//        .subscribe(data -> {
//        System.out.println("client got something!!: " + data);
//        });
//
//        //out
//        return out.send(Flux.just(new Person(111))
//        .map(s -> {
//        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
//        m.writeValue(os, s);
//        return out.alloc()
//        .buffer()
//        .writeBytes(os.toByteArray());
//        } catch (IOException ioe) {
//        throw Exceptions.propagate(ioe);
//        }
//        }));
////			return Mono.empty();
//        }).subscribe();
//        System.out.println("finish");
////assertTrue("Latch was counted down", latch.await(5, TimeUnit.SECONDS));
//
////        connectedClient.dispose();
////        connectedServer.dispose();

//class Person {
//    int x;
//
//    public Person(int x) {
//        this.x = x;
//    }
//
//    public int getX() {
//        return x;
//    }
//
//    @Override
//    public String toString() {
//        return "Person{" +
//                "x=" + x +
//                '}';
//    }
//}