Feature: create get and delete active torrents

  Scenario Outline: we create active torrent
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we delete torrent files only
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "false" and file: "true"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we delete active torrent only
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "false"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we delete active torrent and file
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "true"
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we save pieces of active torrent and read it
    # we can't use "Then application create active-torrent for" because we don't have Flux<PieceMessage> to give yet.
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 1          | 0    | 1      |
      | 3          | 0    |        |
      | 4          | 100  | 100    |
      | -1         | 100  | 100    |
      | -2         | 0    |        |
    Then the only completed pieces are - for torrent: "<torrent>":
      | 3  |
      | -2 |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we save a block which is too large than the corresponding actual piece.
    # we expect that it will be as saving a piece when we don't specify "length".
    # we can't use "Then application create active-torrent for" because we don't have Flux<PieceMessage> to give yet.
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length     |
      | -3         | 0    | 1000000000 |
      | 1          | 30   | 1000000000 |
      | 3          | 0    |            |
    Then the only completed pieces are - for torrent: "<torrent>":
      | -3 |
      | 3  |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: we save all the pieces and expect to see that the fluxes are completed
    When application save the all the pieces of torrent: "<torrent>","<downloadLocation>"
    And the saved-pieces-flux send complete signal - for torrent: "<torrent>","<downloadLocation>"
    And the saved-blocks-flux send  complete signal - for torrent: "<torrent>","<downloadLocation>"

    Examples:
      | torrent                                   | downloadLocation |
      | ComplexFolderStructure.torrent            | torrents-test    |
