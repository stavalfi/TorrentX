package com.utils;

public class PeerFakeError {
    private final String peerIp;
    private final int peerPort;
    private final PeerMessageType sendMessageType;
    private final ErrorSignalType errorSignalType;

    public PeerFakeError(String peerIp, int peerPort, PeerMessageType sendMessageType, ErrorSignalType errorSignalType) {
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        this.sendMessageType = sendMessageType;
        this.errorSignalType = errorSignalType;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public PeerMessageType getSendMessageType() {
        return sendMessageType;
    }

    public ErrorSignalType getErrorSignalType() {
        return errorSignalType;
    }
}
