Feature: create get and delete active torrents

  Scenario Outline: we create active torrent
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |

  Scenario Outline: we delete torrent files only
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "false" and file: "true"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |

  Scenario Outline: we delete active torrent only
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "false"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |

  Scenario Outline: we delete active torrent and file
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>": "true" and file: "true"
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |

  Scenario Outline: we save piece of active torrent
    # we can't use "Then application create active-torrent for" because we don't have Flux<PieceMessage> to give yet.
    When application save random blocks for torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 100  | 100    |
      | 3          | 0    |        |
      | 4          | 100  | 100    |
      | -1         | 100  | 100    |
      | -2         | 0    |        |
      | -7         | 100  | 100    |
      | -6         | 100  |        |
    Then the only completed pieces are - for torrent: "<torrent>":
      | 0  |
      | 3  |
      | -2 |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |


  Scenario Outline: we save all the pieces and expect to see that the fluxes are completed
    Given torrent: "<torrent>","<downloadLocation>"
    When application save the all the pieces of torrent: "<torrent>"
    Then the saved pieces flux send complete signal - for torrent: "<torrent>"
    Then the saved blocks flux send  complete signal - for torrent: "<torrent>"
#    Then torrent-status for torrent "<torrent>" will be:
#      | PAUSE_DOWNLOAD        |
#      | COMPLETED_DOWNLOADING |

    Examples:
      | torrent                                   | downloadLocation |
      | torrent-file-example1.torrent             | torrents-test    |
      | torrent-file-example2.torrent             | torrents-test    |
      | torrent-file-example3.torrent             | torrents-test    |
      | multiple-active-seeders-torrent-1.torrent | torrents-test    |


