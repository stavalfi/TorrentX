Feature: connect to choking fake-peers and map between them and their pieces

#  Scenario Outline: get available pieces from multiple fake-peers while only one is choking me
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#      | 10 |
#    Then fake-peer on port "4042" choke me: "true" - for torrent: "<torrent>"
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following available pieces - for torrent: "<torrent>":
#      | 0 |
#      | 1 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#
#  Scenario Outline: get available pieces from multiple fake-peers while all are choking me
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#      | 10 |
#    Then fake-peer on port "4040" choke me: "true" - for torrent: "<torrent>"
#    Then fake-peer on port "4041" choke me: "true" - for torrent: "<torrent>"
#    Then fake-peer on port "4042" choke me: "true" - for torrent: "<torrent>"
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the none available pieces - for torrent: "<torrent>"
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get available pieces from multiple fake-peers while one is choking and unchoking me
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#      | 10 |
#    Then fake-peer on port "4040" choke me: "true" - for torrent: "<torrent>"
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following available pieces - for torrent: "<torrent>":
#      | 0 |
#      | 1 |
#    Then fake-peer on port "4040" choke me: "false" - for torrent: "<torrent>"
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following extra available pieces - for torrent: "<torrent>":
#      | -1 |
#      | 10 |
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get available fake-peers for specific piece while a single fake-peer is choking me
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#      | 10 |
#    Then fake-peer on port "4040" choke me: "true" - for torrent: "<torrent>"
#    When application request available peers for piece: "0" - for torrent: "<torrent>"
#    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
#      | 4041 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get available fake-peers for specific piece while a single fake-peer is choking and unchoking me
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#      | 10 |
#    Then fake-peer on port "4040" choke me: "true" - for torrent: "<torrent>"
#    When application request available peers for piece: "0" - for torrent: "<torrent>"
#    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
#      | 4041 |
#
#    Then fake-peer on port "4040" choke me: "false" - for torrent: "<torrent>"
#    Then application receive the following extra available fake-peers for piece: "0" - for torrent: "<torrent>":
#      | 4040 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
