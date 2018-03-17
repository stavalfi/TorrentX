Feature: create get and delete active torrents

  Scenario: we create active torrent
    Then application create active-torrent for: <torrent>,<downloadLocation>
    Then active-torrent exist for torrent: <torrent>
    Then folder with files exist for torrent: <torrent> here: <downloadLocation>

  Example:
  | torrent                       | downloadLocation  |
  | torrent-file-example1.torrent | C:\torrents-test\ |
  | torrent-file-example2.torrent | C:\torrents-test\ |


  Scenario: we delete active torrent only
    Then application create active-torrent for: <torrent>,<downloadLocation>
    Then application delete active-torrent: <torrent>
    Then folder with files exist for torrent: <torrent> here: <downloadLocation>

  Example:
  | torrent                       | downloadLocation  |
  | torrent-file-example1.torrent | C:\torrents-test\ |
  | torrent-file-example2.torrent | C:\torrents-test\ |

  Scenario: we delete active torrent and file
    Then application create active-torrent for: <torrent>,<downloadLocation>
    Then application delete active-torrent: <torrent> and file: <downloadLocation>
    Then active-torrent is not exist: <torrent>
    Then file is not exist: <torrent> <downloadLocation>

  Example:
  | torrent                       | downloadLocation  |
  | torrent-file-example1.torrent | C:\torrents-test\ |
  | torrent-file-example2.torrent | C:\torrents-test\ |

  Scenario: we save random blocks inside files. if a piece filled, we check it has been marked as downloaded
    Then application create active-torrent for: <torrent>,<downloadLocation>
    Then application save a random block inside torrent: <torrent> in <downloadLocation> and check it saved
      | pieceIndex | from | to |
      | 0          | 0    | 10 |
      | 1          | 0    | 10 |
      | 0          | 11   | 20 |
    Then application save a random block inside torrent: <torrent> in <downloadLocation> and check it saved
      | pieceIndex | from | to |
      | 1          | 0    | 10 |
      | 1          | 11   | 20 |
      | 1          | 12   |    |
    Then application save a random block inside torrent: <torrent> in <downloadLocation> and check it saved
      | pieceIndex | from | to |
      | 2          | 0    |    |
    Then completed pieces are:
      | pieceIndex |
      | 1          |
      | 2          |

  Example:
  | torrent                       | downloadLocation  |
  | torrent-file-example1.torrent | C:\torrents-test\ |

  Scenario: we save piece of active torrent
    Then application create active-torrent for: <torrent>,<downloadLocation>
    Then application save a random block inside the file and read it for torrent: <torrent> in <downloadLocation>
      | pieceIndex | from | to |
      | 0          | 0    |    |
    Then application save a random block inside the file and read it for torrent: <torrent> in <downloadLocation>
      | pieceIndex | from | to |
      | 1          | 0    | 1  |
    Then completed pieces are:
      | pieceIndex |
      | 0          |

  Example:
  | torrent                       | downloadLocation  |
  | torrent-file-example1.torrent | C:\torrents-test\ |

