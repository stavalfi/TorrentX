package main;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.joou.UShort;

import static org.joou.Unsigned.ushort;

import java.net.InetAddress;

@AllArgsConstructor
@Getter
@ToString
public class Peer {
    private final InetAddress ipAddress;
    private final UShort tcpPort;


}
