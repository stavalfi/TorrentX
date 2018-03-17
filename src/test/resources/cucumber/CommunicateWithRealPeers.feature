Feature: connect to real peers and communicate with them

  Background: read torrent file
    Given new torrent file: "torrent-file-example3.torrent"

  Scenario: we send request block of a piece and we receive it
    Then application interested in all peers
    Then application request for a random block of a random piece from all peers
    Then application receive at list one random block of a random piece