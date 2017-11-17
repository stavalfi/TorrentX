package main.response;

import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@ToString
public class ConnectionResponse {
    private final int action=0;
    private final int transactionId;
    private final long ConnectionId;

    /** test we successfully connected
     * The server must return us a UDP packet with the following structure:
     *
     * Offset  Size            Name            Value
     * 0       32-bit integer  action          0 // connect
     * 4       32-bit integer  transaction_id
     * 8       64-bit integer  connection_id
     * 16
     */
    public ConnectionResponse(byte[] receiveData)
    {
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        assert this.action==receiveData_analyze.getInt();
        this.transactionId =  receiveData_analyze.getInt();
        this.ConnectionId =  receiveData_analyze.getLong();
    }
    public static int packetResponseSize()
    {
        return 100;
    }
}