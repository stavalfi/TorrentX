Feature: remove torrent and files

  Scenario Outline: (1) start remove torrent while we didn't start to download and upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS":
      | REMOVE_TORRENT_IN_PROGRESS |
      | PAUSE_DOWNLOAD_WIND_UP     |
      | PAUSE_UPLOAD_WIND_UP       |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start remove files while we didn't start to download and upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS":
      | REMOVE_FILES_IN_PROGRESS |
      | PAUSE_DOWNLOAD_WIND_UP   |
      | PAUSE_UPLOAD_WIND_UP     |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) start remove files when download is completed - in progress
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | true  |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS":
      | REMOVE_FILES_IN_PROGRESS          |
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (4) start remove files after download is completed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | true  |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS":
      | REMOVE_FILES_IN_PROGRESS      |
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | COMPLETED_DOWNLOADING_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) start remove files after torrent removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | true  |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP   |
      | PAUSE_DOWNLOAD_WIND_UP   |
      | PAUSE_UPLOAD_WIND_UP     |
      | REMOVE_TORRENT_WIND_UP   |
      | REMOVE_FILES_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (6) start remove files after torrent is starting to complete and torrent is starting to be removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | true  |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | REMOVE_TORRENT_WIND_UP            |
      | REMOVE_FILES_IN_PROGRESS          |
      | REMOVE_TORRENT_IN_PROGRESS        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (7) start remove torrent while we download and getting ready to resume upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | false |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | true  |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | true  |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | false |
      | RESUME_UPLOAD_IN_PROGRESS         | true  |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | false |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP     |
      | RESUME_DOWNLOAD_WIND_UP    |
      | START_UPLOAD_WIND_UP       |
      | RESUME_UPLOAD_IN_PROGRESS  |
      | REMOVE_TORRENT_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (8) wind up remove torrent while we already downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | false |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | true  |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | START_DOWNLOAD_WIND_UP     |
      | RESUME_DOWNLOAD_WIND_UP    |
      | PAUSE_UPLOAD_WIND_UP       |
      | REMOVE_TORRENT_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |


  Scenario Outline: (9) wind up remove torrent while we already uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | true  |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | false |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | true  |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_WIND_UP       |
      | RESUME_UPLOAD_WIND_UP      |
      | REMOVE_TORRENT_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (10) wind up remove torrent while we started to complete downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | true  |
      | COMPLETED_DOWNLOADING_WIND_UP     | false |
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | REMOVE_TORRENT_IN_PROGRESS        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (11) wind up remove torrent while we already completed downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS       | false |
      | RESUME_DOWNLOAD_WIND_UP           | false |
      | START_UPLOAD_IN_PROGRESS          | false |
      | START_UPLOAD_WIND_UP              | false |
      | PAUSE_UPLOAD_IN_PROGRESS          | false |
      | PAUSE_UPLOAD_WIND_UP              | true  |
      | RESUME_UPLOAD_IN_PROGRESS         | false |
      | RESUME_UPLOAD_WIND_UP             | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS | false |
      | COMPLETED_DOWNLOADING_WIND_UP     | true  |
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | COMPLETED_DOWNLOADING_WIND_UP |
      | REMOVE_TORRENT_IN_PROGRESS    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |


  Scenario Outline: (12) wind up remove torrent while we already listening
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS                     | false |
      | START_DOWNLOAD_WIND_UP                         | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS                     | false |
      | PAUSE_DOWNLOAD_WIND_UP                         | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS                    | false |
      | RESUME_DOWNLOAD_WIND_UP                        | false |
      | START_UPLOAD_IN_PROGRESS                       | false |
      | START_UPLOAD_WIND_UP                           | false |
      | PAUSE_UPLOAD_IN_PROGRESS                       | false |
      | PAUSE_UPLOAD_WIND_UP                           | true  |
      | RESUME_UPLOAD_IN_PROGRESS                      | false |
      | RESUME_UPLOAD_WIND_UP                          | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS              | false |
      | COMPLETED_DOWNLOADING_WIND_UP                  | false |
      | REMOVE_TORRENT_IN_PROGRESS                     | true  |
      | REMOVE_TORRENT_WIND_UP                         | false |
      | REMOVE_FILES_IN_PROGRESS                       | false |
      | REMOVE_FILES_WIND_UP                           | false |
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | START_LISTENING_TO_INCOMING_PEERS_WIND_UP      | true  |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP     | true  |
      | START_SEARCHING_PEERS_IN_PROGRESS              | false |
      | START_SEARCHING_PEERS_WIND_UP                  | false |
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS              | false |
      | PAUSE_SEARCHING_PEERS_WIND_UP                  | true  |
      | RESUME_SEARCHING_PEERS_IN_PROGRESS             | false |
      | RESUME_SEARCHING_PEERS_WIND_UP                 | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP                     |
      | PAUSE_UPLOAD_WIND_UP                       |
      | REMOVE_TORRENT_IN_PROGRESS                 |
      | START_LISTENING_TO_INCOMING_PEERS_WIND_UP  |
      | RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP              |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (12) wind up remove torrent while we paused everything
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS                     | false |
      | START_DOWNLOAD_WIND_UP                         | false |
      | PAUSE_DOWNLOAD_IN_PROGRESS                     | false |
      | PAUSE_DOWNLOAD_WIND_UP                         | true  |
      | RESUME_DOWNLOAD_IN_PROGRESS                    | false |
      | RESUME_DOWNLOAD_WIND_UP                        | false |
      | START_UPLOAD_IN_PROGRESS                       | false |
      | START_UPLOAD_WIND_UP                           | false |
      | PAUSE_UPLOAD_IN_PROGRESS                       | false |
      | PAUSE_UPLOAD_WIND_UP                           | true  |
      | RESUME_UPLOAD_IN_PROGRESS                      | false |
      | RESUME_UPLOAD_WIND_UP                          | false |
      | COMPLETED_DOWNLOADING_IN_PROGRESS              | false |
      | COMPLETED_DOWNLOADING_WIND_UP                  | false |
      | REMOVE_TORRENT_IN_PROGRESS                     | true  |
      | REMOVE_TORRENT_WIND_UP                         | false |
      | REMOVE_FILES_IN_PROGRESS                       | false |
      | REMOVE_FILES_WIND_UP                           | false |
      | START_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | START_LISTENING_TO_INCOMING_PEERS_WIND_UP      | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS  | false |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP      | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS_IN_PROGRESS | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS_WIND_UP     | false |
      | START_SEARCHING_PEERS_IN_PROGRESS              | false |
      | START_SEARCHING_PEERS_WIND_UP                  | false |
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS              | false |
      | PAUSE_SEARCHING_PEERS_WIND_UP                  | true  |
      | RESUME_SEARCHING_PEERS_IN_PROGRESS             | false |
      | RESUME_SEARCHING_PEERS_WIND_UP                 | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP                    |
      | PAUSE_UPLOAD_WIND_UP                      |
      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP             |
      | REMOVE_TORRENT_WIND_UP                    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |