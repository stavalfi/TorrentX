package main.tracker.response;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class ConnectResponse extends TrackerResponse {
    private final int action;
    private final int transactionId;
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
    public ConnectResponse(String ip, int port,ByteBuffer receiveData) {
        super(ip, port);
        this.action = receiveData.getInt();
        assert this.action == 0;
        this.transactionId = receiveData.getInt();
        this.ConnectionId = receiveData.getLong();
    }

    public static int packetResponseSize() {
        return 1000;
    }
}
