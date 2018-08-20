Feature: connect to a fake peers and communicate with them

  Scenario Outline: (1) we send peer-messages and must receive the same peer-messages back
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything

    Then application send to [peer ip: "localhost", peer port: "8981"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | RequestMessage     |                 |
      | RequestMessage  | PieceMessage       |                 |

    Then application send to [peer ip: "localhost", peer port: "8982"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | BitFieldMessage | BitFieldMessage    |                 |
      | CancelMessage   | CancelMessage      |                 |

    Then application send to [peer ip: "localhost", peer port: "8983"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType   | receiveMessageType | errorSignalType |
      | HaveMessage       | HaveMessage        |                 |
      | InterestedMessage | InterestedMessage  |                 |

    Then application send to [peer ip: "localhost", peer port: "8984"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType      | receiveMessageType   | errorSignalType |
      | KeepAliveMessage     | KeepAliveMessage     |                 |
      | NotInterestedMessage | NotInterestedMessage |                 |

    Then application send to [peer ip: "localhost", peer port: "8985"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | UnchokeMessage  | UnchokeMessage     |                 |
      | PortMessage     | PortMessage        |                 |

    Then application send to [peer ip: "localhost", peer port: "8986"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | RequestMessage     |                 |
      | PieceMessage    | RequestMessage     |                 |

    Then application send to [peer ip: "localhost", peer port: "8987"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | RequestMessage  | PieceMessage       |                 |
      | RequestMessage  | PieceMessage       |                 |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (2) we send 3 peer-messages and the connection must be closed by the rules of the fake peers
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Then application send to [peer ip: "localhost", peer port: "8988"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | PortMessage     | PortMessage        |                 |
      | CancelMessage   | CancelMessage      |                 |
#      | HaveMessage     |                    | EOFException    |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (3) we send 3 peer-messages and the connection must be closed by the rules of the fake peers
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Then application send to [peer ip: "localhost", peer port: "8988"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | RequestMessage  | PieceMessage       |                 |
      | CancelMessage   | CancelMessage      |                 |
#      | UnchokeMessage  |                    | EOFException    |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (4) we send 3 request-messages and the connection must be closed by the rules of the fake peers
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Then application send to [peer ip: "localhost", peer port: "8989"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | RequestMessage  | PieceMessage       |                 |
      | RequestMessage  | PieceMessage       |                 |
#      | RequestMessage  |                    | EOFException    |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (5) we send 3 piece-messages and the connection must be closed by the rules of the fake peers
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything
    Then application send to [peer ip: "localhost", peer port: "8990"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | RequestMessage     |                 |
      | PieceMessage    | RequestMessage     |                 |
#      | PieceMessage    |                    | EOFException    |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

    # race condition.
  Scenario Outline: (6) fake peer request pieces from me and I give him what he want
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 0    | 100    |
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
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (7) fake peer send invalid requests for pieces and I give him what he want
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 0    | 10     |
      | 2          | 0    |        |
    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
      | pieceIndex | from | length     |
      | 0          | 0    | 1000000000 |
      | 0          | 0    | 1000000000 |
      | 0          | 0    | 1000000000 |
      | 0          | 0    | 1000000000 |
      | 0          | 0    | 1000000000 |
      | 0          | 0    | 1000000000 |
      | 1          | 0    | 10         |
      | 2          | 30   | 100        |
    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
      | pieceIndex | from | length |
      | 0          | 0    | -1     |
      | 0          | 0    | -1     |
      | 0          | 0    | -1     |
      | 0          | 0    | -1     |
      | 0          | 0    | -1     |
      | 0          | 0    | -1     |
      | 2          | 30   | 100    |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (8) fake peer request pieces from me but I don't have nothing to give
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
      | pieceIndex | from | length |
      | 0          | 0    | 25     |
      | 1          | 0    | 10     |
      | 2          | 0    | 15     |
    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
      | pieceIndex | from | length |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |

  Scenario Outline: (0) download blocks from a valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    When application request the following blocks from all fake-peers - for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
    Then application receive the following blocks from all - for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |

    Examples:
      | torrent                        | downloadLocation |
      | ComplexFolderStructure.torrent | torrents-test    |