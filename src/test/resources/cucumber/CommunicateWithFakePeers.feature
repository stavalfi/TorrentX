Feature: (3) connect to a fake peers and communicate with them

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
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
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