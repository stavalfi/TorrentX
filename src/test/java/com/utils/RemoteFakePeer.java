package com.utils;

import main.Peer;

public class RemoteFakePeer extends Peer {

    public RemoteFakePeer(Peer peer) {
        super(peer.getPeerIp(), peer.getPeerPort());
    }

    public void listen() {

    }

    public void shutdown() {

    }
}
