package main.tracker.response;

import main.App;
import main.peer.Peer;
import main.tracker.Tracker;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.joou.Unsigned.ushort;

public class AnnounceResponse extends TrackerResponse {

	private final int interval;
	private final int leechersAmount;
	private final int seedersAmount;
	private final List<Peer> peers;

	public Flux<Peer> getPeersFlux() {
		return Flux.fromIterable(this.peers)
				.flatMap(peer -> Mono.just(peer).subscribeOn(App.MyScheduler));
	}

	/**
	 * Offset      Size            Name            Value
	 * 0           32-bit integer  action          1 // scrapeMono
	 * 4           32-bit integer  transaction_id
	 * 8           32-bit integer  interval
	 * 12          32-bit integer  leechersAmount
	 * 16          32-bit integer  seedersAmount
	 * 20 + 6 * n  32-bit integer  IP address
	 * 24 + 6 * n  16-bit integer  TCP port
	 * 20 + 6 * N
	 */
	public AnnounceResponse(Tracker tracker, byte[] response, int maxPeersWeWantToGet) {
		super(tracker);
		ByteBuffer receiveData = ByteBuffer.wrap(response);
		setActionNumber(receiveData.getInt());
		assert getActionNumber() == 1;
		setTransactionId(receiveData.getInt());
		this.interval = receiveData.getInt();
		this.leechersAmount = receiveData.getInt();
		this.seedersAmount = receiveData.getInt();

		this.peers = new ArrayList<>();
		for (int i = 0; i < Integer.min(maxPeersWeWantToGet, this.leechersAmount + this.seedersAmount); i++) {
			try {
				int peerIp = receiveData.getInt();
				InetAddress inetAddress = castIntegerToInetAddress(peerIp);
				String hostAddress = inetAddress.getHostAddress();
				int peerPort = ushort(receiveData.getShort()).intValue();
				Peer peer = new Peer(hostAddress, peerPort);
				this.peers.add(peer);
			} catch (UnknownHostException e) {
				// I will skip this peer and not add it to the list.
			}
		}
	}

	private static InetAddress castIntegerToInetAddress(int ip) throws UnknownHostException {
		byte[] bytes = BigInteger.valueOf(ip).toByteArray();
		return InetAddress.getByAddress(bytes);
	}

	public static int packetResponseSize() {
		return 10000;
	}

	@Override
	public String toString() {
		return "AnnounceResponse{" +
				"interval=" + interval +
				", leechersAmount=" + leechersAmount +
				", seedersAmount=" + seedersAmount +
				", peers=" + peers +
				"} " + super.toString();
	}

	public int getInterval() {
		return interval;
	}

	public int getLeechersAmount() {
		return leechersAmount;
	}

	public int getSeedersAmount() {
		return seedersAmount;
	}
}
