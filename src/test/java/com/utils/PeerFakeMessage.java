package com.utils;

public class PeerFakeMessage {
    private final String peerIp;
    private final int peerPort;
    private final PeerMessageType sendMessageType;
    private final PeerMessageType receiveMessageType;
    private final ErrorSignalType errorSignalType;

    public PeerFakeMessage(String peerIp, int peerPort, PeerMessageType sendMessageType, PeerMessageType receiveMessageType, ErrorSignalType errorSignalType) {
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        this.sendMessageType = sendMessageType;
        this.receiveMessageType = receiveMessageType;
        this.errorSignalType = errorSignalType;
        assert (this.errorSignalType != null && this.receiveMessageType == null) ||
                (this.errorSignalType == null && this.receiveMessageType != null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PeerFakeMessage)) return false;

        PeerFakeMessage that = (PeerFakeMessage) o;

        if (getPeerPort() != that.getPeerPort()) return false;
        return getPeerIp().equals(that.getPeerIp());
    }

    @Override
    public int hashCode() {
        int result = getPeerIp().hashCode();
        result = 31 * result + getPeerPort();
        return result;
    }


    public ErrorSignalType getErrorSignalType() {
        return errorSignalType;
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

    public PeerMessageType getReceiveMessageType() {
        return receiveMessageType;
    }
}
