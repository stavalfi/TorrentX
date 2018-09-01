Feature: (13) start/stop downloading/uploading with side effects

  Scenario Outline: (1) start search
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "RESUME_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | RESUME_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP  |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (2) start search and then try to resume
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "START_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "RESUME_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP         |
      | PAUSE_UPLOAD_WIND_UP           |
      | RESUME_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP  |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (3) start search and then remove files
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "START_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES_IN_PROGRESS |
    Then wait until action is: "REMOVE_FILES_WIND_UP" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_FILES_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | REMOVE_FILES_WIND_UP          |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: 4) start search and then remove torrent
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "START_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT_IN_PROGRESS |
    Then wait until action is: "REMOVE_TORRENT_WIND_UP" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

  Scenario Outline: (5) resume search and remove torrent concurrently
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS_IN_PROGRESS |
    Then wait until action is: "START_SEARCHING_PEERS_WIND_UP" for torrent: "<torrent>"
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
      | REMOVE_TORRENT_IN_PROGRESS         |
    Then wait until action is: "REMOVE_TORRENT_WIND_UP" for torrent: "<torrent>"
    Then torrent-status for torrent "<torrent>" will be with action: "REMOVE_TORRENT_WIND_UP":
      | PAUSE_DOWNLOAD_WIND_UP        |
      | PAUSE_UPLOAD_WIND_UP          |
      | PAUSE_SEARCHING_PEERS_WIND_UP |
      | START_SEARCHING_PEERS_WIND_UP |
      | REMOVE_TORRENT_WIND_UP        |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |

#  Scenario Outline: (6) resume search and then get two peers
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
#    Then start search and receive "2" peers from search-module for torrent: "<torrent>"
#
#    Examples:
#      | torrent     | downloadLocation |
#      | multiple-active-seeders-torrent-1.torrent | torrents-test    |