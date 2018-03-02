Feature: initialize connection between application with random peer

  Background: read torrent file and connect to a random tracker
    Given new torrent file: "torrent-file-example3.torrent"

  Scenario: we send handshake message and must receive handshake back
    Then application send and receive Handshake from the same random peer