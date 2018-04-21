Feature: connect to valid fake-peers and map between them and their pieces

#  Scenario Outline: get available pieces from multiple fake-peers
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
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following available pieces - for torrent: "<torrent>":
#      | 0  |
#      | 1  |
#      | -1 |
#      | 10 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get updated available pieces from single fake-peer
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0  |
#      | -1 |
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following available pieces - for torrent: "<torrent>":
#      | 0  |
#      | -1 |
#    When fake-peer on port "4040" notify on more completed pieces using "BitFieldMessage" - for torrent: "<torrent>":
#      | 0  |
#      | -1 |
#    Then application receive none extra available pieces - for torrent: "<torrent>"
#
#    When fake-peer on port "4040" notify on more completed pieces using "HaveMessage" - for torrent: "<torrent>":
#      | 100 |
#      | -10 |
#    Then application receive the following extra available pieces - for torrent: "<torrent>":
#      | 100 |
#      | -10 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get updated available pieces from multiple fake-peer
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0  |
#      | -1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#    When application request available pieces - for torrent: "<torrent>"
#    Then application receive the following available pieces - for torrent: "<torrent>":
#      | 0  |
#      | -1 |
#    When fake-peer on port "4040" notify on more completed pieces using "HaveMessage" - for torrent: "<torrent>":
#      | 1  |
#      | -1 |
#    When fake-peer on port "4041" notify on more completed pieces using "BitFieldMessage" - for torrent: "<torrent>":
#      | 10 |
#      | -1 |
#    Then application receive the following extra available pieces - for torrent: "<torrent>":
#      | 1  |
#      | 10 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get available fake-peers for specific piece
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
#    When application request available peers for piece: "0" - for torrent: "<torrent>"
#    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
#      | 4040 |
#      | 4041 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: no available fake-peers for specific piece because no fake-peer have it
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
#    When application request available peers for piece: "-2" - for torrent: "<torrent>"
#    Then application receive none available fake-peers for piece: "-2" - for torrent: "<torrent>"
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: get available fake-peers for specific piece while a peer notify later that he have that piece
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | -1 |
#    When application request available peers for piece: "0" - for torrent: "<torrent>"
#    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
#      | 4040 |
#    When fake-peer on port "4041" notify on more completed pieces using "HaveMessage" - for torrent: "<torrent>":
#      | 0 |
#    Then application receive the following extra available pieces - for torrent: "<torrent>":
#      | 4041 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |
#
#  Scenario Outline: no fake-peers for specific piece and then a peer notify later that he have that piece
#    Given torrent: "<torrent>","<downloadLocation>"
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
#      | -2 |
#    When application request available peers for piece: "-1" - for torrent: "<torrent>"
#    Then application receive none available fake-peers for piece: "-1" - for torrent: "<torrent>"
#
#    When fake-peer on port "4041" notify on more completed pieces using "BitFieldMessage" - for torrent: "<torrent>":
#      | -1 |
#    Then application receive the following extra available pieces - for torrent: "<torrent>":
#      | 4041 |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |