package com.utils;

import java.util.Optional;

public class PeerFakeRequestResponse {
    private final PeerMessageType sendMessageType;
    private final PeerMessageType receiveMessageType;
    private final ErrorSignalType errorSignalType;

    public PeerFakeRequestResponse(PeerMessageType sendMessageType, PeerMessageType receiveMessageType, ErrorSignalType errorSignalType) {
        this.sendMessageType = sendMessageType;
        this.receiveMessageType = receiveMessageType;
        this.errorSignalType = errorSignalType;
    }

    public Optional<ErrorSignalType> getErrorSignalType() {
        return Optional.ofNullable(this.errorSignalType);
    }

    public PeerMessageType getSendMessageType() {
        return this.sendMessageType;
    }

    public Optional<PeerMessageType> getReceiveMessageType() {
        return Optional.ofNullable(this.receiveMessageType);
    }
}
