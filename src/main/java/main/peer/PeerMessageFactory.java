package main.peer;

import main.peer.peerMessages.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PeerMessageFactory {
    private static final int bitFieldMessage = 5;
    private static final int cancelMessage = 8;
    private static final int chokeMessage = 0;
    private static final int haveMessage = 4;
    private static final int interestedMessage = 2;
    private static final int keepAliveMessage = 10;
    private static final int notInterestedMessage = 3;
    private static final int pieceMessage = 7;
    private static final int portMessage = 9;
    private static final int requestMessage = 6;
    private static final int unchokeMessage = 1;

    public static PeerMessage create(Peer from, Peer to, InputStream inputStream) throws IOException {
        byte[] data = new byte[4];
        boolean isPeerClosedConnection = (inputStream.read(data) == -1);
        if (isPeerClosedConnection)
            throw new IOException("the peer closed the socket:" + from.toString());
        int lengthOfTheRest = ByteBuffer.wrap(data).getInt();// how much do we need to read more
        data = new byte[lengthOfTheRest];
        isPeerClosedConnection = (inputStream.read(data) == -1);
        if (isPeerClosedConnection)
            throw new IOException("the peer closed the socket:" + from.toString());
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + lengthOfTheRest);
        byteBuffer.putInt(lengthOfTheRest);
        byteBuffer.put(data);
        return create(from, to, byteBuffer.array());
    }

    public static PeerMessage create(Peer from, Peer to, byte[] peerMessage) {
        int messageId = PeerMessage.getMessageId(peerMessage);
        switch (messageId) {
            case bitFieldMessage:
                return new BitFieldMessage(from, to, peerMessage);
            case cancelMessage:
                return new CancelMessage(from, to, peerMessage);
            case chokeMessage:
                return new ChokeMessage(from, to, peerMessage);
            case haveMessage:
                return new HaveMessage(from, to, peerMessage);
            case interestedMessage:
                return new InterestedMessage(from, to, peerMessage);
            case keepAliveMessage:
                return new KeepAliveMessage(from, to, peerMessage);
            case notInterestedMessage:
                return new NotInterestedMessage(from, to, peerMessage);
            case pieceMessage:
                return new PieceMessage(from, to, peerMessage);
            case portMessage:
                return new PortMessage(from, to, peerMessage);
            case requestMessage:
                return new RequestMessage(from, to, peerMessage);
            case unchokeMessage:
                return new UnchokeMessage(from, to, peerMessage);
            default:
                throw new IllegalArgumentException("illegal message id: " + messageId);
        }
    }
}
