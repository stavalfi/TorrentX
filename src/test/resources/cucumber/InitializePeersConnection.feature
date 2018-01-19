Feature: initialize connection between application with random peer.

  Background: read torrent file and connect to a random tracker.
    Given new torrent file: "torrent-file-example1.torrent".
    Then application send and receive the following messages from a random tracker:
      | trackerRequestType | errorSignalType |
      | Connect            |                 |
      | Announce           |                 |

#  Scenario: we send handshake message and must receive handshake back.
#    Then application send Handshake request to a random peer.
#    Then application receive Handshake response from the same peer.
#
#  Scenario: we send invalid torrent-info-hash inside the handshake message
#  to a peer and connection must be terminated by the peer.
#    Then change the torrent-info-hash to a invalid torrent-info-hash.
#    Then application send Handshake request to a random peer.
#    Then communication with the peer failed: "SocketException".