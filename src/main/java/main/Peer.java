package main;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.net.InetAddress;

@AllArgsConstructor
@Getter
@ToString
public class Peer
{
    private final InetAddress ipAddress;
    private final short tcpPort;
}
