package com.utils;

import christophedetroyer.torrent.TorrentFile;
import christophedetroyer.torrent.TorrentParser;
import lombok.SneakyThrows;
import main.TorrentInfo;
import main.file.system.ActiveTorrentFile;
import main.peer.PeersCommunicator;
import main.peer.peerMessages.PeerMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static TorrentInfo readTorrentFile(String torrentFilePath) throws IOException {
        String torrentFilesPath = "src/test/resources/" + torrentFilePath;

        return new TorrentInfo(torrentFilesPath, TorrentParser.parseTorrent(torrentFilesPath));
    }

    public static Mono<PeersCommunicator> sendFakeMessage(PeersCommunicator peersCommunicator, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return peersCommunicator.sendHaveMessage(0);
            case PortMessage:
                return peersCommunicator.sendPortMessage((short) peersCommunicator.getMe().getPeerPort());
            case ChokeMessage:
                return peersCommunicator.sendChokeMessage();
            case PieceMessage:
                return peersCommunicator.sendPieceMessage(0, 0, new byte[10]);
            case CancelMessage:
                return peersCommunicator.sendCancelMessage(0, 0, 10);
            case KeepAliveMessage:
                return peersCommunicator.sendKeepAliveMessage();
            case RequestMessage:
                return peersCommunicator.sendRequestMessage(0, 0, 10);
            case UnchokeMessage:
                return peersCommunicator.sendUnchokeMessage();
            case BitFieldMessage:
                return peersCommunicator.sendBitFieldMessage(BitSet.valueOf(new byte[10]));
            case InterestedMessage:
                return peersCommunicator.sendInterestedMessage();
            case NotInterestedMessage:
                return peersCommunicator.sendNotInterestedMessage();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static Flux<? extends PeerMessage> getSpecificMessageResponseFluxByMessageType(PeersCommunicator peersCommunicator, PeerMessageType peerMessageType) {
        switch (peerMessageType) {
            case HaveMessage:
                return peersCommunicator.receivePeerMessages().getHaveMessageResponseFlux();
            case PortMessage:
                return peersCommunicator.receivePeerMessages().getPortMessageResponseFlux();
            case ChokeMessage:
                return peersCommunicator.receivePeerMessages().getChokeMessageResponseFlux();
            case PieceMessage:
                return peersCommunicator.receivePeerMessages().getPieceMessageResponseFlux();
            case CancelMessage:
                return peersCommunicator.receivePeerMessages().getCancelMessageResponseFlux();
            case KeepAliveMessage:
                return peersCommunicator.receivePeerMessages().getKeepMessageResponseFlux();
            case RequestMessage:
                return peersCommunicator.receivePeerMessages().getRequestMessageResponseFlux();
            case UnchokeMessage:
                return peersCommunicator.receivePeerMessages().getUnchokeMessageResponseFlux();
            case BitFieldMessage:
                return peersCommunicator.receivePeerMessages().getBitFieldMessageResponseFlux();
            case InterestedMessage:
                return peersCommunicator.receivePeerMessages().getInterestedMessageResponseFlux();
            case NotInterestedMessage:
                return peersCommunicator.receivePeerMessages().getNotInterestedMessageResponseFlux();
            default:
                throw new IllegalArgumentException(peerMessageType.toString());
        }
    }

    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
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
        String fullFilePath = downloadPath + "/" + torrentInfo.getName() + "/";
        long position = 0;
        for (TorrentFile torrentFile : fileList) {
            String completeFilePath = torrentFile.getFileDirs()
                    .stream()
                    .collect(Collectors.joining("/", fullFilePath, ""));
            long from = position;
            long to = position + torrentFile.getFileLength();
            position = to;

            ActiveTorrentFile activeTorrentFile = new ActiveTorrentFile(completeFilePath, from, to);
            activeTorrentFileList.add(activeTorrentFile);
        }

        // read from the file

        byte[] result = new byte[requestMessage.getBlockLength()];
        int resultFreeIndex = 0;
        long from = requestMessage.getIndex() * torrentInfo.getPieceLength() + requestMessage.getBegin();
        long to = requestMessage.getIndex() * torrentInfo.getPieceLength() + requestMessage.getBegin() + requestMessage.getBlockLength();

        for (ActiveTorrentFile activeTorrentFile : activeTorrentFileList) {
            if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                RandomAccessFile randomAccessFile = new RandomAccessFile(activeTorrentFile.getDownloadPath(), "rw");
                randomAccessFile.seek(from);
                if (activeTorrentFile.getTo() < to) {
                    byte[] tempResult = new byte[(int) (activeTorrentFile.getTo() - from)];
                    randomAccessFile.read(tempResult);
                    for (int i = 0; i < tempResult.length; i++)
                        result[resultFreeIndex++] = tempResult[i];
                } else {
                    byte[] tempResult = new byte[(int) (to - from)];
                    randomAccessFile.read(tempResult);
                    for (int i = 0; i < tempResult.length; i++)
                        result[resultFreeIndex++] = tempResult[i];
                    return result;
                }
            }
        }
        throw new Exception("we shouldn't be here - never!");
    }
}
