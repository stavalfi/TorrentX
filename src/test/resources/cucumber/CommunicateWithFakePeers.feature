Feature: connect to a fake peers and communicate with them

  Scenario: we send peer-messages and must receive the same peer-messages back
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Given new torrent file: "tor.torrent"
    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | BitFieldMessage | BitFieldMessage    |                 |
      | CancelMessage   | CancelMessage      |                 |

    Then application send to [peer ip: "localhost", peer port: "8981"] and receive the following messages:
      | sendMessageType   | receiveMessageType | errorSignalType |
      | HaveMessage       | HaveMessage        |                 |
      | InterestedMessage | InterestedMessage  |                 |

    Then application send to [peer ip: "localhost", peer port: "8982"] and receive the following messages:
      | sendMessageType      | receiveMessageType   | errorSignalType |
      | KeepAliveMessage     | KeepAliveMessage     |                 |
      | NotInterestedMessage | NotInterestedMessage |                 |

    Then application send to [peer ip: "localhost", peer port: "8983"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | PieceMessage       |                 |
      | RequestMessage  | RequestMessage     |                 |

    Then application send to [peer ip: "localhost", peer port: "8984"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | UnchokeMessage  | UnchokeMessage     |                 |
      | PortMessage     | PortMessage        |                 |

  Scenario: we send 3 peer-messages and the connection must be closed by the rules of the fake peers
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Given new torrent file: "tor.torrent"
    Then application send to [peer ip: "localhost", peer port: "8985"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType |
      | BitFieldMessage | BitFieldMessage    |                 |
      | BitFieldMessage | BitFieldMessage    |                 |
      # the last request will cause the peer to close the connection and it leads to EOFException which we ignore
      # inside the receive() flux. so we will get a complete signal from receive().
      | BitFieldMessage |                    |                 |

  Scenario Outline: fake peer request pieces from me and I give him what he want
    Then application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 0    | 10     |
      | 2          | 0    |        |
    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
      | pieceIndex | from | length |
      | 0          | 0    | 25     |
      | 1          | 0    | 10     |
      | 2          | 0    | 15     |
    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
      | pieceIndex | from | length |
      | 0          | 0    | 25     |
      | 2          | 0    | 15     |

    Examples:
      | torrent     | downloadLocation |
      | tor.torrent | torrents-test/   |

  Scenario Outline: fake peer request pieces from me but I don't have nothing to give
    Then application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
      | pieceIndex | from | length |
      | 0          | 0    | 25     |
      | 1          | 0    | 10     |
      | 2          | 0    | 15     |
    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
      | pieceIndex | from | length |

    Examples:
      | torrent     | downloadLocation |
      | tor.torrent | torrents-test/   |