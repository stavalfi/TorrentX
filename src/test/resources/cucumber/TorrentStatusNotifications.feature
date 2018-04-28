Feature: notifications of the torrent-status module.

#  Scenario Outline: notify about starting the download.
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD                     | false |
#      | START_UPLOAD                       | false |
#      | RESUME_DOWNLOAD                    | false |
#      | RESUME_UPLOAD                      | false |
#      | COMPLETED_DOWNLOADING              | false |
#      | REMOVE_TORRENT                     | false |
#      | REMOVE_FILES                       | false |
#      | START_LISTENING_TO_INCOMING_PEERS  | false |
#      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
#      | START_SEARCHING_PEERS              | false |
#      | RESUME_SEARCHING_PEERS             | false |
#    Then torrent-status change: "START_DOWNLOAD" and notify only about the changes - for torrent "<torrent>":
#      | START_DOWNLOAD  |
#      | RESUME_DOWNLOAD |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: notify about starting the download.
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD                     | false |
#      | START_UPLOAD                       | false |
#      | RESUME_DOWNLOAD                    | false |
#      | RESUME_UPLOAD                      | false |
#      | COMPLETED_DOWNLOADING              | false |
#      | REMOVE_TORRENT                     | false |
#      | REMOVE_FILES                       | false |
#      | START_LISTENING_TO_INCOMING_PEERS  | false |
#      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
#      | START_SEARCHING_PEERS              | false |
#      | RESUME_SEARCHING_PEERS             | false |
#    Then torrent-status change: "START_UPLOAD" and notify only about the changes - for torrent "<torrent>":
#      | START_UPLOAD  |
#      | RESUME_UPLOAD |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: notify about starting the download.
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD                     | false |
#      | START_UPLOAD                       | false |
#      | RESUME_DOWNLOAD                    | false |
#      | RESUME_UPLOAD                      | false |
#      | COMPLETED_DOWNLOADING              | false |
#      | REMOVE_TORRENT                     | false |
#      | REMOVE_FILES                       | false |
#      | START_LISTENING_TO_INCOMING_PEERS  | false |
#      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
#      | START_SEARCHING_PEERS              | false |
#      | RESUME_SEARCHING_PEERS             | false |
#    Then torrent-status change: "START_LISTENING_TO_INCOMING_PEERS" and notify only about the changes - for torrent "<torrent>":
#      | START_LISTENING_TO_INCOMING_PEERS  |
#      | RESUME_LISTENING_TO_INCOMING_PEERS |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: notify about starting the download.
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD                     | false |
#      | START_UPLOAD                       | false |
#      | RESUME_DOWNLOAD                    | false |
#      | RESUME_UPLOAD                      | false |
#      | COMPLETED_DOWNLOADING              | false |
#      | REMOVE_TORRENT                     | false |
#      | REMOVE_FILES                       | false |
#      | START_LISTENING_TO_INCOMING_PEERS  | false |
#      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
#      | START_SEARCHING_PEERS              | false |
#      | RESUME_SEARCHING_PEERS             | false |
#    Then torrent-status change: "START_SEARCHING_PEERS" and notify only about the changes - for torrent "<torrent>":
#      | START_SEARCHING_PEERS  |
#      | RESUME_SEARCHING_PEERS |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: notify about starting the download.
#    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
#      | START_DOWNLOAD                     | false |
#      | START_UPLOAD                       | false |
#      | RESUME_DOWNLOAD                    | false |
#      | RESUME_UPLOAD                      | false |
#      | COMPLETED_DOWNLOADING              | false |
#      | REMOVE_TORRENT                     | false |
#      | REMOVE_FILES                       | false |
#      | START_LISTENING_TO_INCOMING_PEERS  | false |
#      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
#      | START_SEARCHING_PEERS              | false |
#      | RESUME_SEARCHING_PEERS             | false |
#    Then torrent-status change: "COMPLETED_DOWNLOADING" and notify only about the changes - for torrent "<torrent>":
#      | COMPLETED_DOWNLOADING |
#      | PAUSE_DOWNLOAD        |
#      | PAUSE_SEARCHING_PEERS |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |