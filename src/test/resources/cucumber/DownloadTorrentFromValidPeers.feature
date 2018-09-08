Feature: (18) download torrent from valid peers

  Scenario Outline: (1) download torrent from one valid peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>": - none
    Then application download the the torrent: "<torrent>":
    Then wait until download of torrent is finished - for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

#  Scenario Outline: (2) download is completed automatically because we have all the pieces
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given the following saved pieces - for torrent: "<torrent>": - all
#    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
#      | 0 |
#    Then application download the the torrent: "<torrent>":
#    Then wait until download of torrent is finished - for torrent: "<torrentF>"
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |

#  Scenario Outline: (3) download torrent from multiple valid peers
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given the following saved pieces - for torrent: "<torrent>": - none
#    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
#      | 0 |
#    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
#      | 1 |
#    Then application download the the torrent: "<torrent>":
#    Then wait until download of torrent is finished - for torrent: "<torrent>"
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | ComplexFolderStructure.torrent | torrents-test/   |
#
#  Scenario Outline: (4) download torrent from multiple valid peers which connected to me after I started downloading
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given the following saved pieces - for torrent: "<torrent>": - none
#    Then application download the the torrent: "<torrent>":
#    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "1000" milliseconds - for torrent: "<torrent>"
#      | 0 |
#    Given link to "VALID" - fake-peer on port "4040" without the following pieces - with delay: "1000" milliseconds - for torrent: "<torrent>"
#      | 1 |
#    Then wait until download of torrent is finished - for torrent: "<torrent>"
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | ComplexFolderStructure.torrent | torrents-test/   |