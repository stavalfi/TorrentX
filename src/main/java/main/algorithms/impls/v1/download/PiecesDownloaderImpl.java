package main.algorithms.impls.v1.download;

import main.TorrentInfo;
import main.algorithms.BlockDownloader;
import main.algorithms.PeersToPiecesMapper;
import main.algorithms.PiecesDownloader;
import main.file.system.TorrentFileSystemManager;
import main.peer.PeerExceptions;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class PiecesDownloaderImpl implements PiecesDownloader {
	private TorrentInfo torrentInfo;
	private TorrentStatus torrentStatus;
	private PeersToPiecesMapper peersToPiecesMapper;
	private TorrentFileSystemManager torrentFileSystemManager;
	private BlockDownloader blockDownloader;

	private Flux<Integer> downloadedPiecesFlux;

	public PiecesDownloaderImpl(TorrentInfo torrentInfo,
								TorrentStatus torrentStatus,
								TorrentFileSystemManager torrentFileSystemManager,
								PeersToPiecesMapper peersToPiecesMapper,
								BlockDownloader blockDownloader) {
		this.torrentInfo = torrentInfo;
		this.torrentStatus = torrentStatus;
		this.peersToPiecesMapper = peersToPiecesMapper;
		this.torrentFileSystemManager = torrentFileSystemManager;
		this.blockDownloader = blockDownloader;

		downloadedPiecesFlux = torrentStatus.notifyWhenStartDownloading()
				.flatMapMany(__ -> this.peersToPiecesMapper.getAvailablePiecesFlux())
				.flatMap(pieceIndex -> downloadPieceMono(pieceIndex)
						// couldn't download a block from this piece in the specified time.
						// I will try to download the next piece.
						//.onErrorResume(TimeoutException.class, throwable -> Mono.empty())
						// concurrency = 1 -> how many pieces do I want to try to download concurrently.
						// prefetch = 1 -> the inner stream is a mono so any number is ok.
						, 1, 1)
				.publish()
				.autoConnect(0);
	}

	@Override
	public Mono<Integer> downloadPieceMono(int pieceIndex) {
		System.out.println("start downloading piece: " + pieceIndex);
		final int REQUEST_BLOCK_SIZE = 16_384;
		// TODO remove the cast to integer.
		int pieceLength = (int) this.torrentInfo.getPieceLength(pieceIndex);

		Function<Integer, Integer> requestBlockLength = requestFrom ->
				Math.min(REQUEST_BLOCK_SIZE, pieceLength - requestFrom);

		Flux<Integer> requestBlockFromPosition = Flux.generate(sink -> {
			long requestFrom = this.torrentFileSystemManager.getDownloadedBytesInPieces()[pieceIndex];
			if (requestFrom < pieceLength)
				// TODO: remove the (int)
				sink.next((int) requestFrom);
			else
				sink.complete();
		});

		return requestBlockFromPosition.flatMap(requestFromPosition ->
						this.peersToPiecesMapper.peerSupplierFlux(pieceIndex)
								.index()
								//.doOnNext(link -> System.out.println("trying from: " + link.getT1() + ": " + link.getT2().getPeer()))
								.flatMap(link -> {
									// System.out.println("trying to download piece: " + pieceIndex + ", begin: " + requestFromPosition + ", from: (" + link.getT1() + ") " + link.getT2().getPeer());
									RequestMessage requestMessage = new RequestMessage(link.getT2().getMe(), link.getT2().getPeer(),
											pieceIndex, requestFromPosition, requestBlockLength.apply(requestFromPosition));
									return this.blockDownloader.downloadBlock(link.getT2(), requestMessage)
											.onErrorResume(PeerExceptions.peerNotResponding, throwable -> Mono.empty());
									// concurrency = 1 -> how many peers can I connect to the download the same block concurrently.
									// when the download will end, the stream will be canceled using take(1) operator.
									// prefetch = 1 -> the inner stream is a mono so any number is ok.
								}, 1, 1)
								.doOnNext(torrentPieceChanged -> System.out.println("success: " + torrentPieceChanged))
								// if I can't download this block in this duration,
								// I will skip this piece and try to download the next piece.
								.timeout(Duration.ofMillis(20 * 1000))
								// make sure to cancel this flux when I downloaded the block
								// so I won't try to download the same block from the next peer.
								.take(1)
				// concurrency = 1 -> how many times do I want to download the same block concurrently.
				// (I may connect to different peer in each try).
				// prefetch = 1 -> the inner stream is peers Flux so it means that - ***I'm not sure what does prefetch mean here***.
				, 1, 1)
				.collectList()
				.map(torrentPieceChangedList -> pieceIndex)
				// couldn't download a block from this piece in the specified time.
				.doOnError(TimeoutException.class, throwable -> System.out.println("stop downloading piece: " + pieceIndex));
	}

	@Override
	public Flux<Integer> getDownloadedPiecesFlux() {
		return this.downloadedPiecesFlux;
	}

}
