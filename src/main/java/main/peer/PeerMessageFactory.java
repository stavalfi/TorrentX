package main.peer;

import main.peer.peerMessages.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PeerMessageFactory {
    public static PeerMessage create(PeersCommunicator peersCommunicator,Peer from, Peer to, DataInputStream dataInputStream) throws IOException {
        final int messageLengthSize = 4;
        byte[] data1 = new byte[messageLengthSize];
        dataInputStream.readFully(data1);
        int lengthOfTheRestOfData = ByteBuffer.wrap(data1).getInt(); // how much do we need to read more
        byte[] data2 = new byte[lengthOfTheRestOfData];

        dataInputStream.readFully(data2);

        ByteBuffer byteBuffer = ByteBuffer.allocate(messageLengthSize + lengthOfTheRestOfData);
        byteBuffer.putInt(lengthOfTheRestOfData);
        byteBuffer.put(data2);
        return create(peersCommunicator,from, to, byteBuffer.array()); // initialize message object from byte[]
    }

    public static PeerMessage create(PeersCommunicator peersCommunicator,Peer from, Peer to, byte[] peerMessage) {
        PeerMessageId messageId = PeerMessageId.fromValue(PeerMessage.getMessageId(peerMessage));
        switch (messageId) {
            case bitFieldMessage:
                return new BitFieldMessage(peersCommunicator,from, to, peerMessage);
            case cancelMessage:
                return new CancelMessage(peersCommunicator,from, to, peerMessage);
            case chokeMessage:
                return new ChokeMessage(peersCommunicator,from, to, peerMessage);
            case haveMessage:
                return new HaveMessage(peersCommunicator,from, to, peerMessage);
            case interestedMessage:
                return new InterestedMessage(peersCommunicator,from, to, peerMessage);
            case keepAliveMessage:
                return new KeepAliveMessage(peersCommunicator,from, to, peerMessage);
            case notInterestedMessage:
                return new NotInterestedMessage(peersCommunicator,from, to, peerMessage);
            case pieceMessage:
                return new PieceMessage(peersCommunicator,from, to, peerMessage);
            case portMessage:
                return new PortMessage(peersCommunicator,from, to, peerMessage);
            case requestMessage:
                return new RequestMessage(peersCommunicator,from, to, peerMessage);
            case unchokeMessage:
                return new UnchokeMessage(peersCommunicator,from, to, peerMessage);
            case extendedMessage:
                return new ExtendedMessage(peersCommunicator,from, to, peerMessage);
            default:
                throw new IllegalArgumentException("illegal message-id: " + messageId);
        }
    }
}
