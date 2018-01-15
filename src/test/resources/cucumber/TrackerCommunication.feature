Feature: test tracker api calls.

  Scenario Outline: find any tracker, from all the trackers, which response to: connect,announce and scrape requests.
    Given new torrent file: "<torrentFilePath>".

    Then application send signal "Connect" to tracker.
    Then application receive signal "Connect" from tracker.

    Then application send signal "Announce" to tracker.
    Then application receive signal "Announce" from tracker.

    Then application send signal "Scrape" to tracker.
    Then application receive signal "Scrape" from tracker.

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |

  Scenario Outline: communicating with collection of trackers which contain a not-responding trackers.
    Given new torrent file: "<torrentFilePath>".
    Given extra not-responding trackers to the tracker-list.

    Then application send signal "Connect" to tracker.
    Then application receive signal "Connect" from tracker.

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |

  Scenario Outline: communicating with collection of trackers which contain invalid urls of trackers.
    Given new torrent file: "<torrentFilePath>".
    Given invalid url of a tracker.

    Then application send signal "Connect" to tracker.
    Then application receive error signal "UnknownHostException" from tracker.

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |


# I can't find a fake torrent-info-hash omg
#  Scenario Outline: announcing a tracker with the a fake torrent-info-hash.
#    Given new torrent file: "<torrentFilePath>".
#    Then change the torrent-info-hash to a valid but not exist hash.
#
#    Then application send signal: "Connect".
#    Then application receive signal: "Connect".
#
#    Then application send signal: "Announce".
#    Then application receive signal: "Announce".
#
#    Examples:
#      | torrentFilePath               |
#      | torrent-file-example1.torrent |





