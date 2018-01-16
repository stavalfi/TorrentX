Feature: initialize connection between application with random peer.

  Scenario Outline: we send handshake message and must receive handshake back.
    Given new torrent file: "<torrentFilePath>".
    Then application send signal "Connect" to tracker.
    Then application receive signal "Connect" from tracker.

    Then application send signal "Announce" to tracker.
    Then application receive signal "Announce" from tracker.

    Then application send Handshake request to a random peer.
    Then application receive Handshake response from the same peer: "Success".

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |

  Scenario Outline: we send invalid handshake messages to a peer and connection must be terminated by the peer
    Given new torrent file: "<torrentFilePath>".
    Then application send signal "Connect" to tracker.
    Then application receive signal "Connect" from tracker.

    Then application send signal "Announce" to tracker.
    Then application receive signal "Announce" from tracker.

    # we register on the tracker with a specific peer-id and send the peer a different peer-id
    Then change our peer-id to an unregistered peer-id.
    Then application send Handshake request to a random peer.
    Then application receive Handshake response from the same peer: "Failure".

    Then fix invalid details of the torrent.
    Then change the torrent-info-hash to a invalid torrent-info-hash.
    Then application send Handshake request to a random peer.
    Then application receive Handshake response from the same peer: "Failure".

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |