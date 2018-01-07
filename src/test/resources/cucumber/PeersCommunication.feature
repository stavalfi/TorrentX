Feature: test peer-2-peer messages.

#  Scenario Outline: we send handshake message and must receive handshake back.
#    Given new torrent file: "<torrentFilePath>".
#    When application read trackers for this torrent.
#    Then choose one tracker.
#    When application send tracker-request: CONNECT.
#    When application send tracker-request: ANNOUNCE.
#
#    Then choose one active peer to communicate with.
#
#    When application send to peer a peer-request: "HANDSHAKE".
#    Then application receive from peer a peer-response: "HANDSHAKE".
#
#    Examples:
#      | torrentFilePath               |
#      | torrent-file-example1.torrent |