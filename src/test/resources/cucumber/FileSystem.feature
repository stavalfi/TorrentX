Feature: create get and delete active torrents

  Scenario Outline: we create active torrent
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"

    Examples:
      | torrent                       | downloadLocation  |
      | torrent-file-example1.torrent | C:\torrents-test\ |
      | torrent-file-example2.torrent | C:\torrents-test\ |

  Scenario Outline: we delete active torrent only
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then active-torrent exist: "true" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "true" in "<downloadLocation>"

    Examples:
      | torrent                       | downloadLocation  |
      | torrent-file-example1.torrent | C:\torrents-test\ |
      | torrent-file-example2.torrent | C:\torrents-test\ |

  Scenario Outline: we delete active torrent and file
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application delete active-torrent: "<torrent>" and file: "<downloadLocation>"
    Then active-torrent exist: "false" for torrent: "<torrent>"
    Then files of torrent: "<torrent>" exist: "false" in "<downloadLocation>"

    Examples:
      | torrent                       | downloadLocation  |
      | torrent-file-example1.torrent | C:\torrents-test\ |
      | torrent-file-example2.torrent | C:\torrents-test\ |

  Scenario Outline: we save random blocks inside files and if a piece filled, we check it has been marked as downloaded
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application save random blocks from different threads inside torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    | 10     |
      | 1          | 0    | 10     |
      | 0          | 10   | 11     |
    Then application save random blocks from different threads inside torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 1          | 0    | 10     |
      | 1          | 10   | 2      |
      | 1          | 12   |        |
    Then application save random blocks from different threads inside torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 2          | 0    |        |
    Then completed pieces are for torrent: "<torrent>":
      | 1 |
      | 2 |

    Examples:
      | torrent                       | downloadLocation  |
      | torrent-file-example1.torrent | C:\torrents-test\ |

  Scenario Outline: we save piece of active torrent
    Then application create active-torrent for: "<torrent>","<downloadLocation>"
    Then application save random blocks from different threads inside torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 0          | 0    |        |
    Then application save random blocks from different threads inside torrent: "<torrent>" in "<downloadLocation>" and check it saved
      | pieceIndex | from | length |
      | 1          | 0    | 1      |
    Then completed pieces are for torrent: "<torrent>":
      | 0 |

    Examples:
      | torrent                       | downloadLocation  |
      | torrent-file-example1.torrent | C:\torrents-test\ |