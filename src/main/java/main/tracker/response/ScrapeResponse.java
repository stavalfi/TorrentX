package main.tracker.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
public class ScrapeResponse {

    @Getter
    @ToString
    @AllArgsConstructor
    public class ScrapeResponseForTorrentInfoHash
    {
        /**
         * I do not get torrentInfoHash in the response so I need to understand it from the order of the same request.
         */
        private final String torrentInfoHash;
        private final int complete; // 	The current number of connected seeds.
        private final int downloaded; // The number of times this torrent has been downloaded.
        private final int incomplete; // The current number of connected leechers.

    }

    private final int action=2;
    private final int transactionId;
    private final List<ScrapeResponseForTorrentInfoHash> ScrapeResponseForTorrentInfoHashs;

    /**
     * Offset      Size            Name            Value
     * 0           32-bit integer  action          2 // scrape
     * 4           32-bit integer  transaction_id
     * 8 + 12 * n  32-bit integer  downloaded
     * ?           32-bit integer  complete
     * ?           32-bit integer  incomplete
     * 8 + 12 * N
     *
     * @param receiveData what the tracker sent me back.
     * @param torrentInfoHashs what torrents I asked about in the scrape request.
     *                         The order is importent and I assume that the answer
     *                         will be with the same order as this list.
     */
    public ScrapeResponse(byte[] receiveData,List<String> torrentInfoHashs)
    {
        ByteBuffer receiveData_analyze = ByteBuffer.wrap(receiveData);
        int action = receiveData_analyze.getInt();
        assert this.action == action;
        this.transactionId =  receiveData_analyze.getInt();

        this.ScrapeResponseForTorrentInfoHashs = torrentInfoHashs.stream()
                .map((String torrentInfoHash) -> new ScrapeResponseForTorrentInfoHash
                        (torrentInfoHash,receiveData_analyze.getInt(),receiveData_analyze.getInt(),receiveData_analyze.getInt()))
                .collect(Collectors.toList());
    }
    public static int packetResponseSize()
    {
        return 1000;
    }
}
