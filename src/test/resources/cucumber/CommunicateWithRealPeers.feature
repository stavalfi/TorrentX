Feature: connect to real peers and communicate with them

  # TODO: currently I'm getting choked... need to try agian everytime someone unchoke me.
  # It's complicated for a simple test so I will migrate the test logic to use the
  # bittorrent algorithm later.
#  Scenario Outline: we send request block of a piece and we receive it
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD        | false |
#      | START_UPLOAD          | false |
#      | PAUSE_DOWNLOAD        | false |
#      | RESUME_DOWNLOAD       | false |
#      | PAUSE_UPLOAD          | false |
#      | RESUME_UPLOAD         | false |
#      | COMPLETED_DOWNLOADING | false |
#      | REMOVE_TORRENT        | false |
#      | REMOVE_FILES          | false |
#    Then application interested in all peers for torrent: "<torrent>"
#    Then application request for a random block of a random piece from all peers in torrent: "<torrent>"
#    Then application receive at list one random block of a random piece in torrent: "<torrent>"
#
#    Examples:
#      | torrent     | downloadLocation |
#      | tor.torrent | torrents-test/   |

  Scenario: we connect to all peers and get their bitfield status
  and then check that the list of peers we connected to is given
  to us by reactor
    Then application connect to all peers and assert that we connected to them - for torrent: "torrent-file-example1.torrent"
