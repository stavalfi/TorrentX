Feature: (8) remove torrent and files

  Scenario Outline: (1) start remove torrent while we didn't start to download and upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_IN_PROGRESS    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start remove files while we didn't start to download and upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_IN_PROGRESS      |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) start remove files when download is completed - in progress
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | PAUSE_SEARCHING_PEERS_WIND_UP     |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS" - no side effects:
      | START_DOWNLOAD_WIND_UP            |
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | PAUSE_SEARCHING_PEERS_WIND_UP     |
      | REMOVE_FILES_IN_PROGRESS          |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (4) start remove files after download is actually completed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | COMPLETED_DOWNLOADING_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS" - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | COMPLETED_DOWNLOADING_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_IN_PROGRESS    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) actually remove files after download is actually completed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | COMPLETED_DOWNLOADING_WIND_UP |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_IN_PROGRESS    |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_SELF_RESOLVED |
      | REMOVE_TORRENT_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP" - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (6) start remove files after torrent removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |
      | REMOVE_FILES_IN_PROGRESS      |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (7) start remove files after torrent is starting to complete and torrent is starting to be removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | PAUSE_SEARCHING_PEERS_WIND_UP     |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | REMOVE_TORRENT_IN_PROGRESS        |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_IN_PROGRESS" - no side effects:
      | PAUSE_DOWNLOAD_WIND_UP            |
      | PAUSE_UPLOAD_WIND_UP              |
      | PAUSE_SEARCHING_PEERS_WIND_UP     |
      | COMPLETED_DOWNLOADING_IN_PROGRESS |
      | REMOVE_TORRENT_IN_PROGRESS        |
      | REMOVE_FILES_IN_PROGRESS          |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (8) start remove torrent while we actually download and getting ready to resume upload
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_UPLOAD_WIND_UP          |
      | START_DOWNLOAD_WIND_UP        |
      | RESUME_DOWNLOAD_WIND_UP       |
      | RESUME_UPLOAD_IN_PROGRESS     |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_IN_PROGRESS" - no side effects:
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_UPLOAD_WIND_UP          |
      | START_DOWNLOAD_WIND_UP        |
      | RESUME_DOWNLOAD_WIND_UP       |
      | RESUME_UPLOAD_IN_PROGRESS     |
      | REMOVE_TORRENT_IN_PROGRESS    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (9) wind up remove torrent while we already downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | RESUME_DOWNLOAD_WIND_UP       |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_SELF_RESOLVED |
      | REMOVE_TORRENT_WIND_UP       |
    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE" - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | RESUME_DOWNLOAD_WIND_UP       |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (10) start and wind up remove torrent while we already downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | RESUME_DOWNLOAD_WIND_UP       |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_IN_PROGRESS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_SELF_RESOLVED |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD_WIND_UP |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_WIND_UP |
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP" - no side effects:
      | START_DOWNLOAD_WIND_UP        |
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |