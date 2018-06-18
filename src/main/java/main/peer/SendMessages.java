package main.peer;

import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SendMessages {
    private DataOutputStream dataOutputStream;
    private Runnable closeConnectionMethod;

    public SendMessages(DataOutputStream dataOutputStream, Runnable closeConnectionMethod) {
        this.dataOutputStream = dataOutputStream;
        this.closeConnectionMethod = closeConnectionMethod;
    }

    public Mono<SendMessages> send(PeerMessage peerMessage) {
        return Mono.create((MonoSink<SendMessages> monoSink) -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(4 + peerMessage.getMessageLength());

                buffer.putInt(peerMessage.getMessageLength());
                // when receiving a peerMessage,
                // I first check what is the value of "length".
                // if length == 0 then I don't read any more bytes.
                // so there is no reason to send dummy bytes.
                if (peerMessage.getMessageLength() > 0) {
                    buffer.put(peerMessage.getMessageId());
                    buffer.put(peerMessage.getMessagePayload());
                }
                this.dataOutputStream.write(buffer.array());
                monoSink.success(this);
            } catch (IOException e) {
                this.closeConnectionMethod.run();
                monoSink.error(e);
            }
        }).subscribeOn(Schedulers.parallel());
    }

    public Mono<SendMessages> send(PieceMessage pieceMessage) {
        return Mono.create((MonoSink<SendMessages> monoSink) -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(13);

                int messageLength = pieceMessage.getMessageLength();
                buffer.putInt(messageLength);
                byte messageId = pieceMessage.getMessageId();
                buffer.put(messageId);
                int index = pieceMessage.getIndex();
                buffer.putInt(index);
                int begin = pieceMessage.getBegin();
                buffer.putInt(begin);
                byte[] array = buffer.array();
                this.dataOutputStream.write(array);
                this.dataOutputStream.write(pieceMessage.getAllocatedBlock().getBlock(),
                        pieceMessage.getAllocatedBlock().getOffset(), pieceMessage.getAllocatedBlock().getLength());
                monoSink.success(this);
            } catch (IOException e) {
                this.closeConnectionMethod.run();
                monoSink.error(e);
            }
        }).subscribeOn(Schedulers.parallel());
    }
}
