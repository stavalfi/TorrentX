package main;

import org.joou.UShort;

public class Peer {
    private final String ipAddress;
    private final UShort tcpPort;

    public Peer(String ipAddress, UShort tcpPort) {
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public UShort getTcpPort() {
        return tcpPort;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "ipAddress='" + ipAddress + '\'' +
                ", tcpPort=" + tcpPort +
                '}';
    }
}
