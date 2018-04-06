package main.peer;

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

        return getPeerPort() == peer.getPeerPort() &&
                getPeerIp().equals(peer.getPeerIp());
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
