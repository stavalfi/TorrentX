Feature: connect to a fake peers and communicate with them.
  1. the fake peers response with the same peer-message they received.
  2. the second response will be delayed in 2 seconds.
  3. the third response will cause the peer to shutdown the connection and not responding anything.

  Background: read torrent file.
    Given new torrent file: "torrent-file-example1.torrent".

  Scenario: we send peer-messages and must receive the same peer-messages back.
    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | BitFieldMessage | BitFieldMessage    |                 |
      | CancelMessage   | CancelMessage      |                 |

    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType   | receiveMessageType | errorSignalType |
      | HaveMessage       | HaveMessage        |                 |
      | InterestedMessage | InterestedMessage  |                 |

    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType      | receiveMessageType   | errorSignalType |
      | KeepAliveMessage     | KeepAliveMessage     |                 |
      | NotInterestedMessage | NotInterestedMessage |                 |

    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | PieceMessage       |                 |
      | RequestMessage  | RequestMessage     |                 |

    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | UnchokeMessage  | UnchokeMessage     |                 |
      | PortMessage     | PortMessage        |                 |

  Scenario: we send 3 peer-messages and the connection must be closed by the rules of the fake peers.
    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType              |
      | BitFieldMessage | BitFieldMessage    |                              |
      | BitFieldMessage | BitFieldMessage    |                              |
      | BitFieldMessage |                    | PeerDisconnectedExceptionOMG |
