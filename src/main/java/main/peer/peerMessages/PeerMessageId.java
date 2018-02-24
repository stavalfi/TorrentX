package main.peer.peerMessages;

import java.util.Arrays;

public enum PeerMessageId {
    bitFieldMessage(5),
    cancelMessage(8),
    chokeMessage(0),
    haveMessage(4),
    interestedMessage(2),
    keepAliveMessage(10),
    notInterestedMessage(3),
    pieceMessage(7),
    portMessage(9),
    requestMessage(6),
    unchokeMessage(1),
    extendedMessage(20),;

    public static PeerMessageId fromValue(int messageId) {
        return Arrays.stream(PeerMessageId.values())
                .filter((PeerMessageId peerMessageId) -> peerMessageId.getMessageId() == messageId)
                .findFirst()
                .get();
    }

    private int messageId;

    PeerMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getMessageId() {
        return messageId;
    }
}
