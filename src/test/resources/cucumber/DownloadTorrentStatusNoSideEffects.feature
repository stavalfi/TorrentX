Feature: (6) start/stop downloading/uploading

  Scenario Outline: (1) start downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "START_DOWNLOAD_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_IN_PROGRESS     |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "START_UPLOAD_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_UPLOAD_IN_PROGRESS       |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) start to complete the download while we start resume download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP         |
      | RESUME_DOWNLOAD_IN_PROGRESS    |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "COMPLETED_DOWNLOADING_IN_PROGRESS" - no side effects:
      | START_DOWNLOAD_WIND_UP            |
      | RESUME_DOWNLOAD_IN_PROGRESS       |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | START_SEARCHING_PEERS_WIND_UP     |
      | RESUME_SEARCHING_PEERS_WIND_UP    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (4) change start downloading from in progress to wind up
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_IN_PROGRESS     |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "START_DOWNLOAD_WIND_UP" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) start resume downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_DOWNLOAD_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | RESUME_DOWNLOAD_IN_PROGRESS    |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (6) start resume downloading while started pausing download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_IN_PROGRESS     |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE" - no side effects:
      | PAUSE_DOWNLOAD_IN_PROGRESS     |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |


  Scenario Outline: (7) fail to start pausing download while started resuming download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | RESUME_DOWNLOAD_IN_PROGRESS    |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE" - no side effects:
      | RESUME_DOWNLOAD_IN_PROGRESS    |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (8) start resume download and actually resume download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_DOWNLOAD_WIND_UP" - no side effects:
      | RESUME_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (9) from not started downloading to actually resume download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_DOWNLOAD_WIND_UP" - no side effects:
      | RESUME_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (10) from not started uploading to actually resume upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_UPLOAD_WIND_UP" - no side effects:
      | RESUME_UPLOAD_WIND_UP          |
      | PAUSE_DOWNLOAD_WIND_UP         |
      | START_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (11) from not started uploading & downloading to actually resume upload & download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_UPLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_UPLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_DOWNLOAD_WIND_UP" - no side effects:
      | RESUME_UPLOAD_WIND_UP          |
      | START_UPLOAD_WIND_UP           |
      | RESUME_DOWNLOAD_WIND_UP        |
      | START_DOWNLOAD_WIND_UP         |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (12) start download -> resume download -> start complete download -> pause search -> pause download -> complete download
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_SEARCHING_PEERS_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "COMPLETED_DOWNLOADING_WIND_UP" - no side effects:
      | PAUSE_UPLOAD_WIND_UP          |
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | COMPLETED_DOWNLOADING_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (13) nothing will change because we started remove torrent
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | REMOVE_TORRENT_IN_PROGRESS     |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | REMOVE_TORRENT_IN_PROGRESS     |
      | START_SEARCHING_PEERS_WIND_UP  |
      | RESUME_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

    #TODO: bug - doesn't pass sometimes - https://github.com/UniversityFinalProjects/TorrentX/issues/33
Scenario Outline: (14) pause download -> complete download
  Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
    | PAUSE_UPLOAD_WIND_UP              |
    | START_DOWNLOAD_WIND_UP            |
    | PAUSE_DOWNLOAD_WIND_UP            |
    | PAUSE_SEARCHING_PEERS_WIND_UP     |
    | COMPLETED_DOWNLOADING_IN_PROGRESS |
  When torrent-status for torrent "<torrent>" is trying to change to:
    | COMPLETED_DOWNLOADING_SELF_RESOLVED |
  When torrent-status for torrent "<torrent>" is trying to change to:
    | COMPLETED_DOWNLOADING_WIND_UP |
  Then torrent-status for torrent "<torrent>" will be with action: "COMPLETED_DOWNLOADING_WIND_UP" - no side effects:
    | PAUSE_UPLOAD_WIND_UP          |
    | START_DOWNLOAD_WIND_UP        |
    | PAUSE_DOWNLOAD_WIND_UP        |
    | PAUSE_SEARCHING_PEERS_WIND_UP |
    | COMPLETED_DOWNLOADING_WIND_UP |

  Examples:
    | torrent                       | downloadLocation |
    | torrent-file-example1.torrent | torrents-test    |