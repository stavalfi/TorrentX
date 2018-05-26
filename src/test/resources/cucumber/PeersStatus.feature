Feature: start/stop downloading/uploading

#  Scenario Outline: (1) start search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "START_SEARCHING_PEERS_IN_PROGRESS":
#      | PAUSE_DOWNLOAD_WIND_UP                    |
#      | PAUSE_UPLOAD_WIND_UP                      |
#      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP |
#      | PAUSE_SEARCHING_PEERS_WIND_UP             |
#      | START_SEARCHING_PEERS_IN_PROGRESS         |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (2) actually start search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP            |
#      | PAUSE_UPLOAD_WIND_UP              |
#      | PAUSE_SEARCHING_PEERS_WIND_UP     |
#      | START_SEARCHING_PEERS_IN_PROGRESS |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_SELF_RESOLVED |
#      | START_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "START_SEARCHING_PEERS_WIND_UP":
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#      | START_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (3) self resolve start search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP            |
#      | PAUSE_UPLOAD_WIND_UP              |
#      | PAUSE_SEARCHING_PEERS_WIND_UP     |
#      | START_SEARCHING_PEERS_IN_PROGRESS |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_SELF_RESOLVED |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "START_SEARCHING_PEERS_SELF_RESOLVED":
#      | PAUSE_DOWNLOAD_WIND_UP              |
#      | PAUSE_UPLOAD_WIND_UP                |
#      | PAUSE_SEARCHING_PEERS_WIND_UP       |
#      | START_SEARCHING_PEERS_IN_PROGRESS   |
#      | START_SEARCHING_PEERS_SELF_RESOLVED |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (4) pause search when we already in pause search state
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | PAUSE_SEARCHING_PEERS_IN_PROGRESS   |
#      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED |
#      | PAUSE_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (5) actually start search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS   |
#      | START_SEARCHING_PEERS_SELF_RESOLVED |
#      | START_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "START_SEARCHING_PEERS_WIND_UP":
#      | START_SEARCHING_PEERS_WIND_UP |
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (6) fail to start resume search when we don't actually search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (7) start resume search after we actually started to search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#      | START_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_IN_PROGRESS":
#      | START_SEARCHING_PEERS_WIND_UP      |
#      | PAUSE_DOWNLOAD_WIND_UP             |
#      | PAUSE_UPLOAD_WIND_UP               |
#      | PAUSE_SEARCHING_PEERS_WIND_UP      |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#
#  Scenario Outline: (8) start search -> resume search
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS    |
#      | START_SEARCHING_PEERS_SELF_RESOLVED  |
#      | START_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_WIND_UP":
#      | PAUSE_DOWNLOAD_WIND_UP         |
#      | PAUSE_UPLOAD_WIND_UP           |
#      | START_SEARCHING_PEERS_WIND_UP  |
#      | RESUME_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (9) start search -> resume search (with pauses)
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS    |
#      | START_SEARCHING_PEERS_SELF_RESOLVED  |
#      | START_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | PAUSE_SEARCHING_PEERS_IN_PROGRESS    |
#      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED  |
#      | PAUSE_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#      | PAUSE_SEARCHING_PEERS_IN_PROGRESS    |
#      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED  |
#      | PAUSE_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_WIND_UP":
#      | PAUSE_DOWNLOAD_WIND_UP         |
#      | PAUSE_UPLOAD_WIND_UP           |
#      | START_SEARCHING_PEERS_WIND_UP  |
#      | RESUME_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (10) start listen -> resume listen (with out pauses)
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP                    |
#      | PAUSE_UPLOAD_WIND_UP                      |
#      | PAUSE_LISTENING_TO_INCOMING_PEERS_WIND_UP |
#      | PAUSE_SEARCHING_PEERS_WIND_UP             |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS    |
#      | START_SEARCHING_PEERS_SELF_RESOLVED  |
#      | START_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "RESUME_SEARCHING_PEERS_WIND_UP":
#      | PAUSE_DOWNLOAD_WIND_UP         |
#      | PAUSE_UPLOAD_WIND_UP           |
#      | START_SEARCHING_PEERS_WIND_UP  |
#      | RESUME_SEARCHING_PEERS_WIND_UP |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (11) nothing works because torrent start to be removed
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#      | REMOVE_TORRENT_IN_PROGRESS    |
#    When torrent-status for torrent "<torrent>" is trying to change to:
#      | START_SEARCHING_PEERS_IN_PROGRESS    |
#      | START_SEARCHING_PEERS_SELF_RESOLVED  |
#      | START_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | PAUSE_SEARCHING_PEERS_IN_PROGRESS    |
#      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED  |
#      | PAUSE_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#      | PAUSE_SEARCHING_PEERS_IN_PROGRESS    |
#      | PAUSE_SEARCHING_PEERS_SELF_RESOLVED  |
#      | PAUSE_SEARCHING_PEERS_WIND_UP        |
#      | RESUME_SEARCHING_PEERS_IN_PROGRESS   |
#      | RESUME_SEARCHING_PEERS_SELF_RESOLVED |
#      | RESUME_SEARCHING_PEERS_WIND_UP       |
#
#    Then torrent-status for torrent "<torrent>" will be with action: "INITIALIZE":
#      | PAUSE_DOWNLOAD_WIND_UP        |
#      | PAUSE_UPLOAD_WIND_UP          |
#      | PAUSE_SEARCHING_PEERS_WIND_UP |
#      | REMOVE_TORRENT_IN_PROGRESS    |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |