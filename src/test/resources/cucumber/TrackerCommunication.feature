Feature: test tracker api calls

  Scenario Outline: test connection, announce and scrape requests and responses to one of the trackers.
    Given new torrent file: "<torrentFilePath>"
    When application read trackers for this torrent

    When application send request: "CONNECT"
    Then tracker response with same transaction id

    When application send request: "ANNOUNCE"
    Then tracker response with same transaction id

    When application send request: "SCRAPE"
    Then tracker response with same transaction id
    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |



