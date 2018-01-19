package main;

public class Peer {
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

        if (getPeerPort() != peer.getPeerPort()) return false;
        return getPeerIp().equals(peer.getPeerIp());
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
}
