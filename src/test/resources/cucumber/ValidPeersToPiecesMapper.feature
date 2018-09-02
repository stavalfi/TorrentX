Feature: (17) connect to valid fake-peers and map between them and their pieces

  Scenario Outline: (1) get available pieces from one valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
      | 0          | 4040  |
      | 1          | 4040  |
    Then fake-peers disconnect- for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (2) get available pieces from two valid fake-peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
      | 0          | 4040  |
      | 1          | 4041  |
    Then fake-peers disconnect- for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (3) no fake-peers for specific piece and then a peer notify later that he have that piece
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
      | 0          | 4040  |
    Then fake-peers disconnect- for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (4) I can't find any pieces I don't have yet
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>":
      | 1 |
      | 2 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
    Then fake-peers disconnect- for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (5) I can't find any pieces I don't have yet until a new fake-peer notify he can give me something I don't have
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>":
      | 1 |
      | 2 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | -1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | pieceIndex | peers |
      | -1         | 4040  |
    Then fake-peers disconnect- for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |