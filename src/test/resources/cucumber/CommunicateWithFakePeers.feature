Feature: connect to a fake peers and communicate with them

  Scenario Outline: we send peer-messages and must receive the same peer-messages back
#  1. the fake peers response with the same peer-message they received
#  2. the second response will be delayed in 2 seconds
#  3. the third response will cause the peer to shutdown the connection and not responding anything

    Then application send to [peer ip: "localhost", peer port: "8983"] and receive the following messages for torrent: "<torrent>","<downloadLocation>":
      | sendMessageType | receiveMessageType | errorSignalType |
      | PieceMessage    | PieceMessage       |                 |
#      | RequestMessage  | RequestMessage     |                 |

#    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages for torrent: "<torrent>":
#      | sendMessageType | receiveMessageType | errorSignalType |
#      | BitFieldMessage | BitFieldMessage    |                 |
#      | CancelMessage   | CancelMessage      |                 |
#
#    Then application send to [peer ip: "localhost", peer port: "8981"] and receive the following messages for torrent: "<torrent>":
#      | sendMessageType   | receiveMessageType | errorSignalType |
#      | HaveMessage       | HaveMessage        |                 |
#      | InterestedMessage | InterestedMessage  |                 |
#
#    Then application send to [peer ip: "localhost", peer port: "8982"] and receive the following messages for torrent: "<torrent>":
#      | sendMessageType      | receiveMessageType   | errorSignalType |
#      | KeepAliveMessage     | KeepAliveMessage     |                 |
#      | NotInterestedMessage | NotInterestedMessage |                 |
#
#    Then application send to [peer ip: "localhost", peer port: "8984"] and receive the following messages for torrent: "<torrent>":
#      | sendMessageType | receiveMessageType | errorSignalType |
#      | UnchokeMessage  | UnchokeMessage     |                 |
#      | PortMessage     | PortMessage        |                 |
#
    Examples:
      | torrent                                   | downloadLocation |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
#
#  Scenario Outline: we send 3 peer-messages and the connection must be closed by the rules of the fake peers
##  1. the fake peers response with the same peer-message they received
##  2. the second response will be delayed in 2 seconds
##  3. the third response will cause the peer to shutdown the connection and not responding anything
#    Then application send to [peer ip: "localhost", peer port: "8985"] and receive the following messages for torrent: "<torrent>":
#      | sendMessageType | receiveMessageType | errorSignalType |
#      | BitFieldMessage | BitFieldMessage    |                 |
#      | BitFieldMessage | BitFieldMessage    |                 |
#      # the last request will cause the peer to close the connection and it leads to EOFException which we ignore
#      # inside the receive() flux. so we will get a complete signal from receive().
#      | BitFieldMessage |                    |                 |
#
#    Examples:
#      | torrent                                   |
#      | multiple-active-seeders-torrent-1.torrent |
#
#  Scenario Outline: fake peer request pieces from me and I give him what he want
#    Then application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
#      | 1          | 0    | 100    |
#      | 2          | 0    |        |
#    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
#      | pieceIndex | from | length |
#      | 0          | 0    | 25     |
#      | 1          | 0    | 10     |
#      | 2          | 0    | 15     |
#    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
#      | pieceIndex | from | length |
#      | 0          | 0    | 25     |
#      | 2          | 0    | 15     |
#
#    Examples:
#      | torrent                                   | downloadLocation |
#      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
#
#  Scenario Outline: fake peer send invalid requests for pieces and I give him what he want
#    Then application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
##      | 1          | 0    | 10     |
##      | 2          | 0    |        |
#    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
#      | pieceIndex | from | length     |
#      | 0          | 0    | 1000000000 |
##      | 1          | 0    | 10         |
##      | 2          | 30   | 100        |
#    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
#      | pieceIndex | from | length |
#      # -1 == AllocatedBlock length
#      | 0          | 0    | -1     |
##      | 2          | 30   | 100    |
#
#    Examples:
#      | torrent                                   | downloadLocation |
#      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
#
#  Scenario Outline: fake peer request pieces from me but I don't have nothing to give
#    Then application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
#      | pieceIndex | from | length |
#    Then random-fake-peer connect to me for torrent: "<torrent>" in "<downloadLocation>" and he request:
#      | pieceIndex | from | length |
#      | 0          | 0    | 25     |
#      | 1          | 0    | 10     |
#      | 2          | 0    | 15     |
#    Then we assert that for torrent: "<torrent>", we gave the following pieces to the random-fake-peer:
#      | pieceIndex | from | length |
#
#    Examples:
#      | torrent                                   | downloadLocation |
#      | multiple-active-seeders-torrent-1.torrent | torrents-test    |