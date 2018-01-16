package main;

import org.joou.UShort;

public class Peer {
    private final String ipAddress;
    private final int tcpPort;

    public Peer(String ipAddress, UShort tcpPort) {
        this.ipAddress = ipAddress;
        this.tcpPort = tcpPort.intValue();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getTcpPort() {
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
