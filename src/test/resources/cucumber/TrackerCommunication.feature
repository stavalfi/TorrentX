Feature: test tracker api calls.

  Scenario Outline: test connection, announce and scrape requests and responses to one of the trackers.
    Given new torrent file: "<torrentFilePath>".
    When application read trackers for this torrent.

    Then application send tracker-request: CONNECT.
    Then application send tracker-request: ANNOUNCE.
    Then application send tracker-request: SCRAPE.

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |



