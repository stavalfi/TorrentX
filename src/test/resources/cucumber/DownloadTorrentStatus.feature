Feature: start/stop downloading/uploading

  Scenario Outline: (1) start downloading
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
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_DOWNLOAD_WIND_UP":
      | START_DOWNLOAD_WIND_UP  |
      | RESUME_DOWNLOAD_WIND_UP |
      | PAUSE_UPLOAD_WIND_UP    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start upload
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
      | START_UPLOAD_IN_PROGRESS  |
      | START_UPLOAD_WIND_UP      |
      | RESUME_UPLOAD_IN_PROGRESS |
      | RESUME_UPLOAD_WIND_UP     |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_UPLOAD_WIND_UP":
      | START_UPLOAD_WIND_UP   |
      | RESUME_UPLOAD_WIND_UP  |
      | PAUSE_DOWNLOAD_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) start download and upload
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
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_UPLOAD_WIND_UP":
      | START_DOWNLOAD_WIND_UP  |
      | RESUME_DOWNLOAD_WIND_UP |
      | START_UPLOAD_WIND_UP    |
      | RESUME_UPLOAD_WIND_UP   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (4) start download and upload while torrent is removing
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
      | REMOVE_TORRENT_IN_PROGRESS        | true  |
      | REMOVE_TORRENT_WIND_UP            | false |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP     |
      | PAUSE_UPLOAD_WIND_UP       |
      | REMOVE_TORRENT_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) start download and upload while torrent is removed
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
      | REMOVE_TORRENT_WIND_UP            | true  |
      | REMOVE_FILES_IN_PROGRESS          | false |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP |
      | PAUSE_UPLOAD_WIND_UP   |
      | REMOVE_TORRENT_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (6) start download and upload while files are being removed
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
      | REMOVE_FILES_IN_PROGRESS          | true  |
      | REMOVE_FILES_WIND_UP              | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP   |
      | PAUSE_UPLOAD_WIND_UP     |
      | REMOVE_FILES_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (7) start download and upload while files were removed
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
      | REMOVE_FILES_WIND_UP              | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS  |
      | START_DOWNLOAD_WIND_UP      |
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
      | PAUSE_DOWNLOAD_WIND_UP |
      | PAUSE_UPLOAD_WIND_UP   |
      | REMOVE_FILES_WIND_UP   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (8) resume download and start upload while the status is: complete - in progress
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
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_UPLOAD_WIND_UP":
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_WIND_UP            |
      | START_UPLOAD_WIND_UP              |
      | RESUME_UPLOAD_WIND_UP             |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (9) resume download and start upload while the status is completed
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
      | RESUME_DOWNLOAD_IN_PROGRESS |
      | RESUME_DOWNLOAD_WIND_UP     |
      | START_UPLOAD_IN_PROGRESS    |
      | START_UPLOAD_WIND_UP        |
      | RESUME_UPLOAD_IN_PROGRESS   |
      | RESUME_UPLOAD_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_UPLOAD_WIND_UP":
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | START_UPLOAD_WIND_UP          |
      | RESUME_UPLOAD_WIND_UP         |
      | COMPLETED_DOWNLOADING_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (10) start to complete the download while we start resume download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD_IN_PROGRESS        | false |
      | START_DOWNLOAD_WIND_UP            | true  |
      | PAUSE_DOWNLOAD_IN_PROGRESS        | false |
      | PAUSE_DOWNLOAD_WIND_UP            | false |
      | RESUME_DOWNLOAD_IN_PROGRESS       | true  |
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
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "COMPLETED_DOWNLOADING_IN_PROGRESS":
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_IN_PROGRESS        |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |
