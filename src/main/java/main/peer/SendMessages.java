package main.peer;

import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.PieceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SendMessages {
    private static Logger logger = LoggerFactory.getLogger(SendMessages.class);

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
        });
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

                logger.debug("start writing to stream the metadata of a piece:" + pieceMessage);
                this.dataOutputStream.write(array);
                logger.debug("end writing to stream the metadata of a piece:" + pieceMessage);
                logger.debug("start writing to stream the actual data of a piece:" + pieceMessage);
                this.dataOutputStream.write(pieceMessage.getAllocatedBlock().getBlock(), pieceMessage.getAllocatedBlock().getOffset(), pieceMessage.getAllocatedBlock().getLength());
                logger.debug("end writing to stream the actual data of a piece:" + pieceMessage);
                monoSink.success(this);
            } catch (IOException e) {
                this.closeConnectionMethod.run();
                monoSink.error(e);
            }
        });
    }
}
