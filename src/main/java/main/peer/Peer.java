package main.peer;

import java.util.function.Predicate;

public class Peer implements Comparable<Peer> {
    private final String peerIp;
    private final int peerPort;

    public Peer(String peerIp, int peerPort) {
        this.peerIp = peerIp;
        this.peerPort = peerPort;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public int getPeerPort() {
        return peerPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peer)) return false;

        Peer peer = (Peer) o;

        Predicate<String> isAddressEqual = peerIp ->
                peerIp.equals(this.peerIp) ||
                        (peerIp.equals("127.0.0.1") && this.peerIp.equals("localhost")) ||
                        (peerIp.equals("localhost") && this.peerIp.equals("127.0.0.1"));

        return getPeerPort() == peer.getPeerPort() &&
                isAddressEqual.test(peer.peerIp);
    }

    @Override
    public int hashCode() {
        int result = getPeerIp().hashCode();
        result = 31 * result + getPeerPort();
        return result;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "peerIp='" + peerIp + '\'' +
                ", peerPort=" + peerPort +
                '}';
    }

    @Override
    public int compareTo(Peer peer) {
        if (this.peerIp.compareTo(peer.getPeerIp()) != 0)
            return this.peerIp.compareTo(peer.getPeerIp());
        return Integer.compare(this.peerPort, peer.peerPort);
    }
}
