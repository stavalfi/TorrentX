package main.tracker.response;

import lombok.Getter;
import lombok.ToString;
import main.Peer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.joou.Unsigned.ushort;

@Getter
@ToString
public class AnnounceResponse {


    private final int action = 1;
    private final int transactionId;
    private final int interval;
    private final int leechersAmount;
    private final int seedersAmount;
    private final List<Peer> peers;

    /**
     * Offset      Size            Name            Value
     * 0           32-bit integer  action          1 // announce
     * 4           32-bit integer  transaction_id
     * 8           32-bit integer  interval
     * 12          32-bit integer  leechersAmount
     * 16          32-bit integer  seedersAmount
     * 20 + 6 * n  32-bit integer  IP address
     * 24 + 6 * n  16-bit integer  TCP port
     * 20 + 6 * N
     */
    public AnnounceResponse(byte[] receiveData, int maxPeers) {
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        int action = receiveData_analyze.getInt();
        assert this.action == action;
        this.transactionId = receiveData_analyze.getInt();
        this.interval = receiveData_analyze.getInt();
        this.leechersAmount = receiveData_analyze.getInt();
        this.seedersAmount = receiveData_analyze.getInt();

        this.peers = IntStream.range(0, Integer.min(maxPeers, this.leechersAmount + this.seedersAmount))
                .mapToObj((int index) -> new Peer(castIntegerToInetAddress(receiveData_analyze.getInt()), ushort(receiveData_analyze.getShort())))
                .collect(Collectors.toList());
    }

    public static int packetResponseSize() {
        return 1000;
    }

    private static InetAddress castIntegerToInetAddress(int ip) {
        byte[] bytes = BigInteger.valueOf(ip).toByteArray();
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
}
