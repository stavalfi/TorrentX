package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.App;
import main.TorrentInfo;
import main.downloader.PieceEvent;
import main.downloader.TorrentPieceStatus;
import main.peer.Peer;
import main.peer.peerMessages.BitFieldMessage;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import main.torrent.status.TorrentStatusController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveTorrent extends TorrentInfo implements TorrentFileSystemManager {

	private final List<ActiveTorrentFile> activeTorrentFileList;
	private final BitSet piecesStatus;
	private final long[] downloadedBytesInPieces;
	private final String downloadPath;
	private TorrentStatusController torrentStatusController;
	private Flux<Integer> savedPieceFlux;
	private Flux<PieceEvent> startListenForIncomingPiecesFlux;
	private Mono<ActiveTorrent> notifyWhenActiveTorrentDeleted;
	private Mono<ActiveTorrent> notifyWhenFilesDeleted;

	public ActiveTorrent(TorrentInfo torrentInfo, String downloadPath,
						 TorrentStatusController torrentStatusController,
						 Flux<PieceMessage> peerResponsesFlux) throws IOException {
		super(torrentInfo);
		this.downloadPath = downloadPath;
		this.torrentStatusController = torrentStatusController;
		this.piecesStatus = new BitSet(getPieces().size());
		this.downloadedBytesInPieces = new long[getPieces().size()];

		createFolders(torrentInfo, downloadPath);

		this.activeTorrentFileList = createActiveTorrentFileList(torrentInfo, downloadPath);

		this.notifyWhenActiveTorrentDeleted = this.torrentStatusController
				.notifyWhenFilesRemoved()
				.flatMap(__ -> deleteFileOnlyMono())
				.flux()
				.replay(1)
				.autoConnect(0)
				.single();

		this.notifyWhenFilesDeleted = this.torrentStatusController
				.notifyWhenTorrentRemoved()
				.flatMap(__ -> deleteActiveTorrentOnlyMono())
				.flux()
				.replay(1)
				.autoConnect(0)
				.single();

		this.startListenForIncomingPiecesFlux = peerResponsesFlux
				.filter(pieceMessage -> !havePiece(pieceMessage.getIndex()))
				.flatMap(this::writeBlock)
				.publish()
				.autoConnect(0);

		this.savedPieceFlux = this.startListenForIncomingPiecesFlux
				.filter(torrentPieceChanged -> torrentPieceChanged.getTorrentPieceStatus().equals(TorrentPieceStatus.COMPLETED))
				.map(PieceEvent::getReceivedPiece)
				.map(PieceMessage::getIndex)
				.distinct()
				.publish()
				.autoConnect(0);
	}

	@Override
	public List<? extends main.file.system.TorrentFile> getTorrentFiles() {
		return this.activeTorrentFileList;
	}

	@Override
	public BitSet getUpdatedPiecesStatus() {
		return this.piecesStatus;
	}

	@Override
	public TorrentInfo getTorrentInfo() {
		return this;
	}

	@Override
	public BitFieldMessage buildBitFieldMessage(Peer from, Peer to) {
		return new BitFieldMessage(from, to, this.piecesStatus);
	}

	@Override
	public boolean havePiece(int pieceIndex) {
		return this.piecesStatus.get(pieceIndex);
	}

	@Override
	public String getDownloadPath() {
		return downloadPath;
	}

	@Override
	public Flux<PieceEvent> savedBlockFlux() {
		return this.startListenForIncomingPiecesFlux;
	}

	@Override
	public Flux<Integer> savedPieceFlux() {
		return this.savedPieceFlux;
	}

	public Mono<ActiveTorrent> deleteActiveTorrentOnlyMono() {
		return Flux.fromIterable(this.activeTorrentFileList)
				.flatMap(ActiveTorrentFile::closeFileChannel)
				.collectList()
				.flatMap(activeTorrentFiles -> {
					boolean deletedActiveTorrent = ActiveTorrents.getInstance()
							.deleteActiveTorrentOnly(getTorrentInfoHash());
					if (deletedActiveTorrent)
						return Mono.just(this);
					return Mono.error(new Exception("ActiveTorrent object not exist."));
				});
	}

	public Mono<ActiveTorrent> deleteFileOnlyMono() {
		return Flux.fromIterable(this.activeTorrentFileList)
				.flatMap(ActiveTorrentFile::closeFileChannel)
				.collectList()
				.flatMap(activeTorrentFiles -> {
					if (this.isSingleFileTorrent()) {
						String singleFilePath = this.activeTorrentFileList.get(0).getFilePath();
						return completelyDeleteFolder(singleFilePath);
					}
					// I will delete this file at the next operator.
					String torrentDirectoryPath = this.downloadPath + this.getName();
					return completelyDeleteFolder(torrentDirectoryPath);
				});
	}

	@Override
	public synchronized int minMissingPieceIndex() {
		for (int i = 0; i < this.getPieces().size(); i++)
			if (!this.piecesStatus.get(i))
				return i;
		return -1;
	}

	@Override
	public int maxMissingPieceIndex() {
		for (int i = this.getPieces().size() - 1; i >= 0; i--)
			if (!this.piecesStatus.get(i))
				return i;
		return -1;
	}

	@Override
	public long[] getDownloadedBytesInPieces() {
		return this.downloadedBytesInPieces;
	}

	@Override
	public Mono<PieceMessage> buildPieceMessage(RequestMessage requestMessage) {
//		if (!havePiece(requestMessage.getIndex()))
//			return Mono.error(new PieceNotDownloadedYetException(requestMessage.getIndex()));

		return Mono.<PieceMessage>create(sink -> {
			byte[] result = new byte[requestMessage.getBlockLength()];
			int freeIndexInResultArray = 0;

			long from = super.getPieceStartPosition(requestMessage.getIndex());
			long to = from + requestMessage.getBlockLength();

			for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList) {
				if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
					int howMuchToReadFromThisFile = (int) Math.min(activeTorrentFile.getTo() - from, to - from);
					byte[] tempResult;
					try {
						tempResult = activeTorrentFile.readBlock(from, howMuchToReadFromThisFile);
					} catch (IOException e) {
						sink.error(e);
						return;
					}
					for (byte aTempResult : tempResult)
						result[freeIndexInResultArray++] = aTempResult;

					from += howMuchToReadFromThisFile;
					if (from == to) {
						PieceMessage pieceMessage = new PieceMessage(requestMessage.getTo(), requestMessage.getFrom(),
								requestMessage.getIndex(), requestMessage.getBegin(), result);
						sink.success(pieceMessage);
						return;
					}
				}
			}
		}).subscribeOn(App.MyScheduler);
	}

	private Mono<PieceEvent> writeBlock(PieceMessage pieceMessage) {
		if (havePiece(pieceMessage.getIndex()) ||
				this.downloadedBytesInPieces[pieceMessage.getIndex()] > pieceMessage.getBegin() + pieceMessage.getBlock().length)
			// I already have the received block. I don't need it.
			return Mono.empty();

		return Mono.<PieceEvent>create(sink -> {
			long from = super.getPieceStartPosition(pieceMessage.getIndex()) + pieceMessage.getBegin();
			long to = from + pieceMessage.getBlock().length;

			// from which position the ActiveTorrentFile object needs to write to filesystem from the given block array.
			int arrayIndexFrom = 0;

			for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList)
				if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
					// (to-from)<=piece.length <= file.size , request.length<= Integer.MAX_VALUE
					// so: (Math.min(to, activeTorrentFile.getTo()) - from) <= Integer.MAX_VALUE
					int howMuchToWriteFromArray = (int) Math.min(activeTorrentFile.getTo() - from, to - from);
					try {
						activeTorrentFile.writeBlock(from, pieceMessage.getBlock(), arrayIndexFrom, howMuchToWriteFromArray);
					} catch (IOException e) {
						sink.error(e);
						return;
					}
					// increase 'from' because next time we will write to different position.
					from += howMuchToWriteFromArray;
					arrayIndexFrom += howMuchToWriteFromArray;
					if (from == to)
						break;
				}

			// update pieces status:

			// there maybe multiple writes of the same pieceRequest during one execution...
			long pieceLength = getPieceLength(pieceMessage.getIndex());
			this.downloadedBytesInPieces[pieceMessage.getIndex()] += pieceMessage.getBlock().length;
			if (pieceLength < this.downloadedBytesInPieces[pieceMessage.getIndex()])
				this.downloadedBytesInPieces[pieceMessage.getIndex()] = getPieceLength(pieceMessage.getIndex());

			long howMuchWeWroteUntilNowInThisPiece = this.downloadedBytesInPieces[pieceMessage.getIndex()];
			if (howMuchWeWroteUntilNowInThisPiece >= pieceLength) {
				// update pieces partial status array:
				// TODO: WARNING: this line *only* must be synchronized among multiple threads!
				this.piecesStatus.set(pieceMessage.getIndex());
				PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.COMPLETED, pieceMessage);
				sink.success(pieceEvent);

				if (minMissingPieceIndex() == -1)
					this.torrentStatusController.completedDownloading();
			} else {
				// update pieces partial status array:
				// TODO: WARNING: this line *only* must be synchronized among multiple threads!
				PieceEvent pieceEvent = new PieceEvent(TorrentPieceStatus.DOWNLOADING, pieceMessage);
				sink.success(pieceEvent);
			}
		}).subscribeOn(Schedulers.single());
	}

	private List<ActiveTorrentFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) throws IOException {
		String mainFolder = !torrentInfo.isSingleFileTorrent() ?
				downloadPath + torrentInfo.getName() + File.separator :
				downloadPath;

		// create activeTorrentFile list
		int position = 0;
		List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
		for (TorrentFile torrentFile : torrentInfo.getFileList()) {
			String filePath = torrentFile
					.getFileDirs()
					.stream()
					.collect(Collectors.joining(File.separator, mainFolder, ""));
			SeekableByteChannel seekableByteChannel = createFile(filePath);
			ActiveTorrentFile activeTorrentFile = new ActiveTorrentFile(filePath, position, position + torrentFile.getFileLength().intValue(), seekableByteChannel);
			activeTorrentFileList.add(activeTorrentFile);
			position += torrentFile.getFileLength();
		}
		return activeTorrentFileList;
	}

	private SeekableByteChannel createFile(String filePathToCreate) throws IOException {
		OpenOption[] options = {
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE_NEW,
				StandardOpenOption.SPARSE,
				StandardOpenOption.READ
				// TODO: think if we add CREATE if exist rule.
		};
		SeekableByteChannel seekableByteChannel = Files.newByteChannel(Paths.get(filePathToCreate), options);
		return seekableByteChannel;
	}

	private void createFolders(TorrentInfo torrentInfo, String downloadPath) {
		// create main folder for the download of the torrent.
		String mainFolder = !torrentInfo.isSingleFileTorrent() ?
				downloadPath + torrentInfo.getName() + File.separator :
				downloadPath;
		createFolder(mainFolder);

		// create sub folders for the download of the torrent
		torrentInfo.getFileList()
				.stream()
				.map(christophedetroyer.torrent.TorrentFile::getFileDirs)
				.filter(folders -> folders.size() > 1)
				.map(folders -> folders.subList(0, folders.size() - 2))
				.map(List::stream)
				.map(stringStream -> stringStream.collect(Collectors.joining(File.separator, mainFolder, "")))
				.forEach(folderPath -> createFolder(folderPath));
	}

	private void createFolder(String path) {
		File file = new File(path);
		File parentFile = file.getParentFile();
		parentFile.mkdirs();
		file.mkdirs();
	}

	private Mono<ActiveTorrent> completelyDeleteFolder(String directoryToBeDeleted) {
		try {
			completelyDeleteFolderRecursive(new File(directoryToBeDeleted));
		} catch (IOException e) {
			return Mono.error(e);
		}
		return Mono.just(this);
	}

	private void completelyDeleteFolderRecursive(File directoryToBeDeleted) throws IOException {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				completelyDeleteFolderRecursive(file);
			}
		}
		Files.delete(directoryToBeDeleted.toPath());
	}

	public Mono<ActiveTorrent> getNotifyWhenActiveTorrentDeleted() {
		return this.notifyWhenActiveTorrentDeleted;
	}

	public Mono<ActiveTorrent> getNotifyWhenFilesDeleted() {
		return this.notifyWhenFilesDeleted;
	}
}