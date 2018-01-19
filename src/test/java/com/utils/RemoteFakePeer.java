package com.utils;

import main.peer.Peer;

import java.io.IOException;
import java.io.InputStream;
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

    public RemoteFakePeer(Peer peer) {
        super(peer.getPeerIp(), peer.getPeerPort());
        try {
            this.listenToPeerConnection = new ServerSocket(this.getPeerPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        new Thread(() -> {
            ServerSocket welcomeSocket = null;
            try {
                welcomeSocket = new ServerSocket(6789);
                while (!this.closeEverything) {
                    Socket newPeerConnection = welcomeSocket.accept();
                    this.peerConnections.add(newPeerConnection);
                    waitForMessagesFromPeer(newPeerConnection);
                }
            } catch (Exception e) {

            }
        }).start();
    }

    private void waitForMessagesFromPeer(Socket peerConnection) throws IOException, InterruptedException {
        int receivedMessagesAmount = 0;
        while (!this.closeEverything) {
            InputStream in = peerConnection.getInputStream();
            OutputStream out = peerConnection.getOutputStream();
            byte[] data = new byte[2000];
            in.read(data);
            receivedMessagesAmount++;
            if (receivedMessagesAmount == 2) {
                Thread.sleep(2000);
            } else if (receivedMessagesAmount == 3) {
                peerConnection.close();
                return;
            }
            out.write(data);
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
                e.printStackTrace();
            }
        });
    }
}