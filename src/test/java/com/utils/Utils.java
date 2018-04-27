package com.utils;

import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import lombok.SneakyThrows;
import main.TorrentInfo;
import main.algorithms.BittorrentAlgorithm;
import main.algorithms.impls.BittorrentAlgorithmInitializer;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.file.system.ActiveTorrent;
import main.file.system.ActiveTorrentFile;
import main.file.system.ActiveTorrents;
import main.file.system.TorrentFileSystemManager;
import main.peer.*;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import main.statistics.SpeedStatistics;
import main.statistics.TorrentSpeedSpeedStatisticsImpl;
import main.torrent.status.TorrentStatusController;
import main.torrent.status.TorrentStatusControllerImpl;
import main.tracker.TrackerConnection;
import main.tracker.TrackerProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
	public static PeersListener peersListener;

	public static TorrentInfo createTorrentInfo(String torrentFilePath) throws IOException {
		String torrentFilesPath = "src" + File.separator +
				"test" + File.separator +
				"resources" + File.separator +
				"torrents" + File.separator +
				torrentFilePath;
		return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
	}

	public static void removeEverythingRelatedToLastTest() {

		Mono<List<ActiveTorrent>> activeTorrentsListMono = ActiveTorrents.getInstance()
				.getActiveTorrentsFlux()
				.flatMap(activeTorrent -> activeTorrent.deleteFileOnlyMono()
						// if the test deleted the files then I will get NoSuchFileException and we will not delete the ActiveTorrent object.
						.onErrorResume(Throwable.class, throwable -> Mono.just(activeTorrent)))
				.flatMap(activeTorrent -> activeTorrent.deleteActiveTorrentOnlyMono()
						// if the test deleted the ActiveTorrent object then I may get an exception which I clearly don't want.
						.onErrorResume(Throwable.class, throwable -> Mono.just(activeTorrent)))

				.collectList();
		try {
			activeTorrentsListMono.block();
		} catch (Exception e) {
			e.printStackTrace();
		}


		Mono<List<TorrentStatusController>> torrentDownloadersListMono = TorrentDownloaders.getInstance()
				.getTorrentDownloadersFlux()
				.doOnNext(torrentDownloader -> TorrentDownloaders.getInstance().deleteTorrentDownloader(torrentDownloader.getTorrentInfo().getTorrentInfoHash()))
				.map(TorrentDownloader::getTorrentStatusController)
				.doOnNext(torrentStatusController -> {
					torrentStatusController.pauseDownload();
					torrentStatusController.pauseUpload();
				})
				.collectList();
		try {
			torrentDownloadersListMono.block();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (peersListener != null) {
			try {
				peersListener.stopListenForNewPeers();
			} catch (IOException e) {

			}
			peersListener = null;
		}

		// delete download folder from last test
		Utils.deleteDownloadFolder();
	}

	public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath) {
		return createDefaultTorrentDownloader(torrentInfo, downloadPath,
				TorrentStatusControllerImpl.createDefault(torrentInfo));
	}

	public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
																   Flux<TrackerConnection> trackerConnectionConnectableFlux) {
		TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
		PeersProvider peersProvider = new PeersProvider(torrentInfo);

		TorrentStatusController torrentStatusController =
				TorrentStatusControllerImpl.createDefault(torrentInfo);

		peersListener = new PeersListener(torrentStatusController);

		Flux<Link> searchingPeers$ = torrentStatusController.notifyWhenStartSearchingPeers()
				.flatMapMany(__ ->
						peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
								.autoConnect(0));

		Flux<Link> peersCommunicatorFlux =
				Flux.merge(peersListener.getPeersConnectedToMeFlux()
						// SocketException == When I shutdown the SocketServer after/before
						// the tests inside Utils::removeEverythingRelatedToTorrent.
						.onErrorResume(SocketException.class, throwable -> Flux.empty()), searchingPeers$)
						// multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
						// multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
						// every time and then I will connect to all the peers again and again...
						.publish()
						.autoConnect(0);

		TorrentFileSystemManager torrentFileSystemManager = ActiveTorrents.getInstance()
				.createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusController,
						peersCommunicatorFlux.map(Link::receivePeerMessages)
								.flatMap(ReceivePeerMessages::getPieceMessageResponseFlux))
				.block();

		BittorrentAlgorithm bittorrentAlgorithm =
				BittorrentAlgorithmInitializer.v1(torrentInfo,
						torrentStatusController,
						torrentFileSystemManager,
						peersCommunicatorFlux);

		SpeedStatistics torrentSpeedStatistics =
				new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
						peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

		return TorrentDownloaders.getInstance()
				.createTorrentDownloader(torrentInfo,
						torrentFileSystemManager,
						bittorrentAlgorithm,
						torrentStatusController,
						torrentSpeedStatistics,
						trackerProvider,
						peersProvider,
						trackerConnectionConnectableFlux,
						peersCommunicatorFlux);
	}

	public static TorrentDownloader createDefaultTorrentDownloader(TorrentInfo torrentInfo, String downloadPath,
																   TorrentStatusController torrentStatusController) {
		TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
		PeersProvider peersProvider = new PeersProvider(torrentInfo);

		Flux<TrackerConnection> trackerConnectionConnectableFlux =
				trackerProvider.connectToTrackersFlux()
						.autoConnect();

		peersListener = new PeersListener(torrentStatusController);

		Flux<Link> searchingPeers$ = torrentStatusController.notifyWhenStartSearchingPeers()
				.flatMapMany(__ ->
						peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
								.autoConnect(0));

		Flux<Link> peersCommunicatorFlux =
				Flux.merge(peersListener.getPeersConnectedToMeFlux(), searchingPeers$)
						// multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
						// multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
						// every time and then I will connect to all the peers again and again...
						.publish()
						.autoConnect(0);

		TorrentFileSystemManager torrentFileSystemManager = ActiveTorrents.getInstance()
				.createActiveTorrentMono(torrentInfo, downloadPath, torrentStatusController,
						peersCommunicatorFlux.map(Link::receivePeerMessages)
								.flatMap(ReceivePeerMessages::getPieceMessageResponseFlux))
				.block();

		BittorrentAlgorithm bittorrentAlgorithm =
				BittorrentAlgorithmInitializer.v1(torrentInfo,
						torrentStatusController,
						torrentFileSystemManager,
						peersCommunicatorFlux);

		SpeedStatistics torrentSpeedStatistics =
				new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
						peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

		return TorrentDownloaders.getInstance()
				.createTorrentDownloader(torrentInfo,
						torrentFileSystemManager,
						bittorrentAlgorithm,
						torrentStatusController,
						torrentSpeedStatistics,
						trackerProvider,
						peersProvider,
						trackerConnectionConnectableFlux,
						peersCommunicatorFlux);
	}

	public static TorrentDownloader createCustomTorrentDownloader(TorrentInfo torrentInfo,
																  TorrentStatusController torrentStatusController,
																  TorrentFileSystemManager torrentFileSystemManager,
																  Flux<TrackerConnection> trackerConnectionConnectableFlux) {
		TrackerProvider trackerProvider = new TrackerProvider(torrentInfo);
		PeersProvider peersProvider = new PeersProvider(torrentInfo);

		peersListener = new PeersListener(torrentStatusController);

		Flux<Link> searchingPeers$ = torrentStatusController.notifyWhenStartSearchingPeers()
				.flatMapMany(__ ->
						peersProvider.getPeersCommunicatorFromTrackerFlux(trackerConnectionConnectableFlux)
								.autoConnect(0));

		Flux<Link> incomingPeers$ = peersListener.getPeersConnectedToMeFlux()
				// SocketException == When I shutdown the SocketServer after/before
				// the tests inside Utils::removeEverythingRelatedToTorrent.
				.onErrorResume(SocketException.class, throwable -> Flux.empty());

		Flux<Link> peersCommunicatorFlux =
				Flux.merge(incomingPeers$, searchingPeers$)
						// multiple subscriptions will activate flatMap(__ -> multiple times and it will cause
						// multiple calls to getPeersCommunicatorFromTrackerFlux which create new hot-flux
						// every time and then I will connect to all the peers again and again...
						.publish()
						.autoConnect(0);

		BittorrentAlgorithm bittorrentAlgorithm =
				BittorrentAlgorithmInitializer.v1(torrentInfo,
						torrentStatusController,
						torrentFileSystemManager,
						peersCommunicatorFlux);

		SpeedStatistics torrentSpeedStatistics =
				new TorrentSpeedSpeedStatisticsImpl(torrentInfo,
						peersCommunicatorFlux.map(Link::getPeerSpeedStatistics));

		return TorrentDownloaders.getInstance()
				.createTorrentDownloader(torrentInfo,
						torrentFileSystemManager,
						bittorrentAlgorithm,
						torrentStatusController,
						torrentSpeedStatistics,
						trackerProvider,
						peersProvider,
						trackerConnectionConnectableFlux,
						peersCommunicatorFlux);
	}

	public static Mono<SendPeerMessages> sendFakeMessage(Link link, PeerMessageType peerMessageType) {
		switch (peerMessageType) {
			case HaveMessage:
				return link.sendMessages().sendHaveMessage(0);
			case PortMessage:
				return link.sendMessages().sendPortMessage((short) link.getMe().getPeerPort());
			case ChokeMessage:
				return link.sendMessages().sendChokeMessage();
			case PieceMessage:
				return link.sendMessages().sendPieceMessage(0, 0, new byte[10]);
			case CancelMessage:
				return link.sendMessages().sendCancelMessage(0, 0, 10);
			case KeepAliveMessage:
				return link.sendMessages().sendKeepAliveMessage();
			case RequestMessage:
				return link.sendMessages().sendRequestMessage(0, 0, 10);
			case UnchokeMessage:
				return link.sendMessages().sendUnchokeMessage();
			case BitFieldMessage:
				return link.sendMessages().sendBitFieldMessage(BitSet.valueOf(new byte[10]));
			case InterestedMessage:
				return link.sendMessages().sendInterestedMessage();
			case NotInterestedMessage:
				return link.sendMessages().sendNotInterestedMessage();
			default:
				throw new IllegalArgumentException(peerMessageType.toString());
		}
	}

	public static Flux<? extends PeerMessage> getSpecificMessageResponseFluxByMessageType(Link link, PeerMessageType peerMessageType) {
		switch (peerMessageType) {
			case HaveMessage:
				return link.receivePeerMessages().getHaveMessageResponseFlux();
			case PortMessage:
				return link.receivePeerMessages().getPortMessageResponseFlux();
			case ChokeMessage:
				return link.receivePeerMessages().getChokeMessageResponseFlux();
			case PieceMessage:
				return link.receivePeerMessages().getPieceMessageResponseFlux();
			case CancelMessage:
				return link.receivePeerMessages().getCancelMessageResponseFlux();
			case KeepAliveMessage:
				return link.receivePeerMessages().getKeepMessageResponseFlux();
			case RequestMessage:
				return link.receivePeerMessages().getRequestMessageResponseFlux();
			case UnchokeMessage:
				return link.receivePeerMessages().getUnchokeMessageResponseFlux();
			case BitFieldMessage:
				return link.receivePeerMessages().getBitFieldMessageResponseFlux();
			case InterestedMessage:
				return link.receivePeerMessages().getInterestedMessageResponseFlux();
			case NotInterestedMessage:
				return link.receivePeerMessages().getNotInterestedMessageResponseFlux();
			case ExtendedMessage:
				return link.receivePeerMessages().getExtendedMessageResponseFlux();
			default:
				throw new IllegalArgumentException(peerMessageType.toString());
		}
	}

	public static long folderSize(File directory) {
		long length = 0;
		for (File file : Objects.requireNonNull(directory.listFiles())) {
			if (file.isFile())
				length += file.length();
			else
				length += folderSize(file);
		}
		return length;
	}

	@SneakyThrows
	public static byte[] readFromFile(TorrentInfo torrentInfo, String downloadPath, RequestMessage requestMessage) {
		List<TorrentFile> fileList = torrentInfo.getFileList();

		List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
		String fullFilePath = downloadPath;
		if (!torrentInfo.isSingleFileTorrent())
			fullFilePath += torrentInfo.getName() + File.separator;
		long position = 0;
		for (TorrentFile torrentFile : fileList) {
			String completeFilePath = torrentFile.getFileDirs()
					.stream()
					.collect(Collectors.joining(File.separator, fullFilePath, ""));
			long from = position;
			long to = position + torrentFile.getFileLength();
			position = to;

			ActiveTorrentFile activeTorrentFile = new ActiveTorrentFile(completeFilePath, from, to, null);
			activeTorrentFileList.add(activeTorrentFile);
		}

		// read from the file:

		long from = torrentInfo.getPieceStartPosition(requestMessage.getIndex()) + requestMessage.getBegin();
		long to = from + requestMessage.getBlockLength();

		byte[] result = new byte[requestMessage.getBlockLength()];
		int resultFreeIndex = 0;
		long amountOfBytesOfFileWeCovered = 0;
		for (ActiveTorrentFile activeTorrentFile : activeTorrentFileList) {
			if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
				RandomAccessFile randomAccessFile = new RandomAccessFile(activeTorrentFile.getFilePath(), "r");
				long fromWhereToReadInThisFile = from - amountOfBytesOfFileWeCovered;
				randomAccessFile.seek(fromWhereToReadInThisFile);

				int howMuchToReadFromThisFile = (int) Math.min(activeTorrentFile.getTo() - from, to - from);
				byte[] tempResult = new byte[howMuchToReadFromThisFile];
				randomAccessFile.read(tempResult);
				for (byte aTempResult : tempResult)
					result[resultFreeIndex++] = aTempResult;
				from += howMuchToReadFromThisFile;

				randomAccessFile.close();
			}
			if (from == to)
				return result;
			amountOfBytesOfFileWeCovered = activeTorrentFile.getTo();
		}
		throw new Exception("we shouldn't be here - never!");
	}

	public static void deleteDownloadFolder() {
		try {
			File file = new File(System.getProperty("user.dir") + File.separator + "torrents-test");
			if (file.exists()) {
				deleteDirectory(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void deleteDirectory(File directoryToBeDeleted) throws IOException {
		Files.walkFileTree(directoryToBeDeleted.toPath(), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
