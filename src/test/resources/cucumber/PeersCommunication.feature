Feature: connect to a fake peers and communicate with them.
  1. the fake peers response with the same peer-message they received.
  2. the second response will be delayed in 2 seconds.
  3. the third response will cause the peer to shutdown the connection.

  Background: read a torrent file and create fake servers which represent peers.
    Given new torrent file: "torrent-file-example1.torrent" containing the following fake peers:
      | peerIp    | peerPort |
      | localhost | 8980     |
      | localhost | 8981     |
      | localhost | 8982     |
      | localhost | 8983     |
      | localhost | 8984     |
      | localhost | 8985     |
      | localhost | 8986     |
      | localhost | 8987     |
      | localhost | 8988     |
      | localhost | 8989     |
      | localhost | 8990     |

#  Scenario: we send peer-messages and must receive the same peer-messages back.
#  I don't PortMessage because this indicated I changed my port I'm listening to.
#  It will be checked in different Scenario - this test is too hard, I won't do it.
#
#    Then application send in parallel and receive the following messages:
#      | peerIp    | peerPort | sendMessageType      | receiveMessageType   | errorSignalType |
#      | localhost | 8980     | BitFieldMessage      | BitFieldMessage      |                 |
#      | localhost | 8981     | CancelMessage        | CancelMessage        |                 |
#      | localhost | 8982     | ChokeMessage         | ChokeMessage         |                 |
#      | localhost | 8983     | HaveMessage          | HaveMessage          |                 |
#      | localhost | 8984     | InterestedMessage    | InterestedMessage    |                 |
#      | localhost | 8985     | IsAliveMessage       | IsAliveMessage       |                 |
#      | localhost | 8986     | NotInterestedMessage | NotInterestedMessage |                 |
#      | localhost | 8987     | PieceMessage         | PieceMessage         |                 |
#      | localhost | 8989     | RequestMessage       | RequestMessage       |                 |
#      | localhost | 8990     | UnchokeMessage       | UnchokeMessage       |                 |
#
#  Scenario: we send 3 peer-messages and the connection must be closed by the rules of the fake peers.
#
#    Then application send in parallel and receive the following messages:
#      | peerIp    | peerPort | sendMessageType | receiveMessageType | errorSignalType              |
#      | localhost | 8980     | BitFieldMessage | BitFieldMessage    |                              |
#      | localhost | 8981     | BitFieldMessage | BitFieldMessage    |                              |
#      | localhost | 8982     | BitFieldMessage |                    | PeerDisconnectedExceptionOMG |
