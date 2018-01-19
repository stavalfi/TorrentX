package com.utils;

import main.peer.*;

public enum PeerMessageType {
    BitFieldMessage(main.peer.BitFieldMessage.class),
    CancelMessage(main.peer.CancelMessage.class),
    ChokeMessage(main.peer.ChokeMessage.class),
    HaveMessage(main.peer.HaveMessage.class),
    InterestedMessage(main.peer.InterestedMessage.class),
    IsAliveMessage(IsAliveMessage.class),
    NotInterestedMessage(NotInterestedMessage.class),
    PieceMessage(PieceMessage.class),
    PortMessage(PortMessage.class),
    RequestMessage(RequestMessage.class),
    UnchokeMessage(UnchokeMessage.class);

    final Class<? extends PeerMessage> signal;

    PeerMessageType(Class<? extends PeerMessage> signal) {
        this.signal = signal;
    }

    public Class<? extends PeerMessage> getSignal() {
        return this.signal;
    }
}
