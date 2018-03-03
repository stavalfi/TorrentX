package com.utils;

public class PeerRequestResponseMessage {
    private final PeerMessageType sendMessageType;
    private final PeerMessageType receiveMessageType;

    public PeerRequestResponseMessage(PeerMessageType sendMessageType,
                                      PeerMessageType receiveMessageType) {
        this.sendMessageType = sendMessageType;
        this.receiveMessageType = receiveMessageType;
    }

    public PeerMessageType getSendMessageType() {
        return sendMessageType;
    }

    public PeerMessageType getReceiveMessageType() {
        return receiveMessageType;
    }
}
