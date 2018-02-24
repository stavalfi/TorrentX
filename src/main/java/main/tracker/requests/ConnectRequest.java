package main.tracker.requests;

import main.tracker.Tracker;

import java.nio.ByteBuffer;

public class ConnectRequest extends TrackerRequest {

    private final long connectionId = 0x41727101980L;


    public ConnectRequest(Tracker tracker, int transactionId) {
        super(tracker, 0, transactionId);
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
        sendData.putInt(getActionNumber()); // action we want to perform - connecting with the server (32 bits)
        sendData.putInt(getTransactionId()); // transaction_id - random int we make (32 bits)

        return sendData;
    }

    @Override
    public String toString() {
        return "ConnectRequest{" +
                "connectionId=" + connectionId +
                '}' + super.toString();
    }

    public long getConnectionId() {
        return connectionId;
    }
}
