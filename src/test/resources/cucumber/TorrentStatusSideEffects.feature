Feature: cause and assert that side effects happen

  Scenario Outline: Torrent FileSystem States Side Effects
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS":
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP    |

  Examples:
  | torrent                       | downloadLocation |
  | torrent-file-example1.torrent | torrents-test    |