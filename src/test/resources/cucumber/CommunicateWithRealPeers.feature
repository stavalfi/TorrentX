Feature: connect to real peers and communicate with them

  Background: read torrent file
    Given new torrent file: "torrent-file-example3.torrent"

  Scenario: we send request block of a piece and we receive it
    Then application interested in all peers
    Then application request for a random block of a random piece from all peers
    Then application receive at list one random block of a random piece

  Scenario: we connect to all peers and get their bitfield status
  and then check that the list of peers we connected to is given
  to us by reactor
    Then application connect to all peers and assert that we connected to them - for torrent: "torrent-file-example1.torrent"