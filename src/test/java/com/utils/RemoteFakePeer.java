package com.utils;

import main.peer.Peer;
import main.peer.PeerMessageFactory;
import main.peer.peerMessages.HandShake;
import main.peer.peerMessages.PeerMessage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 1. the fake peers response with the same peer-message they received.
 * 2. the second response will be delayed in 2 seconds.
 * 3. the third response will cause the peer to shutdown the connection.
 */
public class RemoteFakePeer extends Peer {

    private boolean closeEverything = false;
    private ServerSocket listenToPeerConnection;
    private final List<Socket> peerConnections = new ArrayList<>();

    public RemoteFakePeer(Peer Me) {
        super(Me.getPeerIp(), Me.getPeerPort());
        try {
            this.listenToPeerConnection = new ServerSocket(this.getPeerPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        Thread thread = new Thread(() -> {
            try {
                while (!this.closeEverything) {
                    Socket newPeerConnection = this.listenToPeerConnection.accept();
                    this.peerConnections.add(newPeerConnection);
                    waitForMessagesFromPeer(newPeerConnection);
                }
            } catch (Exception e) {
                // I don't want to print errors from this class.
                // a possible error can be if a peer is closing
                // the connection with This Fake peer.
                // e.printStackTrace();
                shutdown();
            }
        });
        thread.setName("Fake Peer - wait for connections & messages");
        thread.start();
    }

    private void waitForMessagesFromPeer(Socket peerConnection) {
        int receivedMessagesAmount = 0;
        while (!this.closeEverything) {
            try {
                DataInputStream dataInputStream = new DataInputStream(peerConnection.getInputStream());
                OutputStream outputStream = peerConnection.getOutputStream();
                if (receivedMessagesAmount == 0) {
                    HandShake handShakeReceived = new HandShake(dataInputStream);
                    outputStream.write(handShakeReceived.createPacketFromObject());
                } else {
                    if (receivedMessagesAmount == 2)
                        Thread.sleep(2000);
                    else if (receivedMessagesAmount == 3) {
                        peerConnection.close();
                        return;
                    }
                    Peer fromPeer = new Peer("localhost", peerConnection.getPort());
                    PeerMessage peerMessage = PeerMessageFactory.create(fromPeer, this, dataInputStream);
                    outputStream.write(peerMessage.createPacketFromObject());
                }
                receivedMessagesAmount++;
            } catch (IOException | InterruptedException e) {
                // I don't want to print errors from this class.
                // a possible error can be if a peer is closing
                // the connection with This Fake peer.
                // e.printStackTrace();
                try {
                    peerConnection.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void shutdown() {
        this.closeEverything = true;
        try {
            this.listenToPeerConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.peerConnections.forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                // I don't want to print errors from this class.
                // a possible error can be if a peer is closing
                // the connection with This Fake peer.
                // e.printStackTrace();
            }
        });
    }
}
//                    if (receivedMessagesAmount == 2)
//                        Thread.sleep(2 * 1000);
//                    if (receivedMessagesAmount == 3) {
//                        try {
//                            peerConnection.close();
//                        } catch (IOException e1) {
//                            e1.printStackTrace();
//                        }
//                        return;
//                    }