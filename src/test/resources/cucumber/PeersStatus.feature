Feature: start/stop downloading/uploading

  Scenario Outline: (1) start to listen while we already downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_WIND_UP                         | true  |
      | PAUSE_DOWNLOAD_WIND_UP                         | false |
      | RESUME_DOWNLOAD_WIND_UP                        | true  |
      | START_UPLOAD_WIND_UP                           | false |
      | PAUSE_UPLOAD_WIND_UP                           | true  |
      | RESUME_UPLOAD_WIND_UP                          | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS              | false |
      | COMPLETED_DOWNLOADING_WIND_UP                  | false |
      | REMOVE_TORRENT_IN_PROGRESS                     | false |
      | REMOVE_TORRENT_WIND_UP                         | false |
      | REMOVE_FILES_IN_PROGRESS                       | false |
      | REMOVE_FILES_WIND_UP                           | false |
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | START_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP     | false |
      | START_SEARCHING_PEERS_IN_PROGRESS              | false |
      | START_SEARCHING_PEERS_WIND_UP                  | false |
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS              | false |
      | PAUSE_SEARCHING_PEERS_WIND_UP                  | false |
      | RESUME_SEARCHING_PEERS_IN_PROGRESS             | false |
      | RESUME_SEARCHING_PEERS_WIND_UP                 | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP                        |
      | RESUME_DOWNLOAD_WIND_UP                       |
      | PAUSE_UPLOAD_WIND_UP                          |
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start to search while we already downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_WIND_UP                         | true  |
      | PAUSE_DOWNLOAD_WIND_UP                         | false |
      | RESUME_DOWNLOAD_WIND_UP                        | true  |
      | START_UPLOAD_WIND_UP                           | false |
      | PAUSE_UPLOAD_WIND_UP                           | true  |
      | RESUME_UPLOAD_WIND_UP                          | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS              | false |
      | COMPLETED_DOWNLOADING_WIND_UP                  | false |
      | REMOVE_TORRENT_IN_PROGRESS                     | false |
      | REMOVE_TORRENT_WIND_UP                         | false |
      | REMOVE_FILES_IN_PROGRESS                       | false |
      | REMOVE_FILES_WIND_UP                           | false |
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | START_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP     | false |
      | START_SEARCHING_PEERS_IN_PROGRESS              | false |
      | START_SEARCHING_PEERS_WIND_UP                  | false |
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS              | false |
      | PAUSE_SEARCHING_PEERS_WIND_UP                  | false |
      | RESUME_SEARCHING_PEERS_IN_PROGRESS             | false |
      | RESUME_SEARCHING_PEERS_WIND_UP                 | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "START_SEARCHING_PEERS_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP            |
      | RESUME_DOWNLOAD_WIND_UP           |
      | PAUSE_UPLOAD_WIND_UP              |
      | START_SEARCHING_PEERS_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |