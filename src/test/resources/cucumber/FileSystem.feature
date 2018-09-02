Feature: (7) create get and delete active torrents

  Scenario Outline: (1) remove torrent concurrently while nothing has started
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then wait until state contain the following for torrent: "<torrent>":
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) remove files concurrently while nothing has started
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then wait until state contain the following for torrent: "<torrent>":
      | REMOVE_FILES_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_WIND_UP          |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) remove files and torrent concurrently while nothing has started
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS   |
      | REMOVE_TORRENT_IN_PROGRESS |
    Then wait until state contain the following for torrent: "<torrent>":
      | REMOVE_FILES_WIND_UP   |
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_WIND_UP          |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (4) remove files and torrent concurrently while started search
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "RESUME_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS   |
      | REMOVE_TORRENT_IN_PROGRESS |
    Then wait until state contain the following for torrent: "<torrent>":
      | REMOVE_FILES_WIND_UP   |
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_WIND_UP          |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) remove files and torrent and resume concurrently
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "RESUME_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "PAUSE_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
      | REMOVE_FILES_IN_PROGRESS           |
      | REMOVE_TORRENT_IN_PROGRESS         |
    Then wait until state contain the following for torrent: "<torrent>":
      | REMOVE_FILES_WIND_UP   |
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_WIND_UP          |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (6) we create active torrent
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: (7) we delete torrent files only
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    # TODO: there is a blocking here which prevent from me to even dispatch windup on remove files
    Then application delete active-torrent: "<torrent>": "false" and file: "true"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_WIND_UP":
      | REMOVE_FILES_WIND_UP          |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: (8) we delete active torrent only
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "false"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | REMOVE_TORRENT_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: (9) we delete active torrent and files twice
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "true"
    When application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "true"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | REMOVE_TORRENT_WIND_UP        |
      | REMOVE_FILES_WIND_UP          |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | REMOVE_TORRENT_WIND_UP        |
      | REMOVE_FILES_WIND_UP          |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |
      | ComplexFolderStructure.torrent            | torrents-test    |

  Scenario Outline: (10) we save pieces of active torrent and read it
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

    # bug: the first example cause block for ever (first step).
  Scenario Outline: (11) we save a block which is too large than the corresponding actual piece.
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

#  Scenario Outline: (12) we save all the pieces and expect to see that the fluxes are completed
#    When application save the all the pieces of torrent: "<torrent>","<downloadLocation>"
#    And the saved-pieces-flux send complete signal - for torrent: "<torrent>","<downloadLocation>"
#    And the saved-blocks-flux send  complete signal - for torrent: "<torrent>","<downloadLocation>"
#
#    Examples:
#      | torrent                        | downloadLocation |
#      | ComplexFolderStructure.torrent | torrents-test    |

