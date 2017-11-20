package main.peer;

import main.NaturalX;

public class PortMessage extends Message {
    public PortMessage(int listenPort) {
        super(3, 9, new NaturalX.Natural2(listenPort).buffer());
    }
}
