package com.utils;

import main.peer.peerMessages.*;

public enum PeerMessageType {
    BitFieldMessage(main.peer.peerMessages.BitFieldMessage.class),
    CancelMessage(main.peer.peerMessages.CancelMessage.class),
    ChokeMessage(main.peer.peerMessages.ChokeMessage.class),
    HaveMessage(main.peer.peerMessages.HaveMessage.class),
    InterestedMessage(main.peer.peerMessages.InterestedMessage.class),
    KeepAliveMessage(KeepAliveMessage.class),
    NotInterestedMessage(NotInterestedMessage.class),
    PieceMessage(PieceMessage.class),
    PortMessage(PortMessage.class),
    RequestMessage(RequestMessage.class),
    UnchokeMessage(UnchokeMessage.class),
    ExtendedMessage(ExtendedMessage.class);

    final Class<? extends PeerMessage> signal;

    PeerMessageType(Class<? extends PeerMessage> signal) {
        this.signal = signal;
    }

    public Class<? extends PeerMessage> getSignal() {
        return this.signal;
    }
}
