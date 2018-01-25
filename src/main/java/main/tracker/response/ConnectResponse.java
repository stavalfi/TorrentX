package main.tracker.response;

import java.nio.ByteBuffer;

public class ConnectResponse extends TrackerResponse {
    private final long ConnectionId;

    /**
     * test we successfully connected
     * The server must return us a UDP packet with the following structure:
     * <p>
     * Offset  Size            Name            Value
     * 0       32-bit integer  action          0 // connect
     * 4       32-bit integer  transaction_id
     * 8       64-bit integer  connection_id
     * 16
     */
    public ConnectResponse(String ip, int port, byte[] response) {
        super(ip, port);
        ByteBuffer receiveData = ByteBuffer.wrap(response);
        setActionNumber(receiveData.getInt());
        assert getActionNumber() == 0;
        setTransactionId(receiveData.getInt());
        this.ConnectionId = receiveData.getLong();
    }

    public static int packetResponseSize() {
        return 1000;
    }

    public long getConnectionId() {
        return ConnectionId;
    }

    @Override
    public String toString() {
        return "ConnectResponse{" +
                "ConnectionId=" + ConnectionId +
                "} " + super.toString();
    }
}
