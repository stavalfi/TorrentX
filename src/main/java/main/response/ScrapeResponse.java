package main.response;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class ScrapeResponse {
    private final int action=2;
    private final int transactionId;
    private final int seedersAmount;
    private final int completed;
    private final int leechersAmount;

    /**
     * Offset      Size            Name            Value
     * 0           32-bit integer  action          2 // scrape
     * 4           32-bit integer  transaction_id
     * 8 + 12 * n  32-bit integer  seedersAmount
     * 12 + 12 * n 32-bit integer  completed
     * 16 + 12 * n 32-bit integer  leechersAmount
     * 8 + 12 * N
     */
    public ScrapeResponse(byte[] receiveData)
    {
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        assert this.action==receiveData_analyze.getInt();
        this.transactionId =  receiveData_analyze.getInt();
        this.seedersAmount =  receiveData_analyze.getInt();
        this.completed =  receiveData_analyze.getInt();
        this.leechersAmount =  receiveData_analyze.getInt();
    }
    public static int packetResponseSize()
    {
        return 100;
    }
}
