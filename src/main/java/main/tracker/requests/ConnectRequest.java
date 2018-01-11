package main.tracker.requests;

import lombok.Getter;
import lombok.ToString;
import main.tracker.response.ConnectResponse;

import java.nio.ByteBuffer;
import java.util.function.Function;

@Getter
@ToString
public class ConnectRequest extends TrackerRequest {

    private final long connectionId = 0x41727101980L;
    private final int action = 0;
    private final int transactionId;


    public ConnectRequest(String ip, int port, int transactionId) {
        super(ip, port);
        this.transactionId = transactionId;
    }

    /**
     * offset == bytes not bits!!!!!!
     * Offset  Size            Name            Value
     * 0       64-bit integer  connection_id   0x41727101980
     * 8       32-bit integer  action          0 // connect
     * 12      32-bit integer  transaction_id  random (we decide)
     * 16
     */
    @Override
    public ByteBuffer buildRequestPacket() {

        ByteBuffer sendData = ByteBuffer.allocate(128);
        sendData.putLong(this.connectionId); // connection_id - can't change (64 bits)
        sendData.putInt(this.action); // action we want to perform - connecting with the server (32 bits)
        sendData.putInt(this.transactionId); // transaction_id - random int we make (32 bits)

        return sendData;
    }
}
