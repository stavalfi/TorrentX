Feature: (16) connect to valid fake-peers and download a piece from them

  Scenario Outline: (1) download one piece from valid peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (2) download two pieces from valid peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (3) download one piece from valid peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (4) download multiple pieces from multiple valid peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0  |
      | 1  |
      | -1 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0  |
      | 1  |
      | -1 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (5) download one piece from valid peer who send less data then requested every time
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "SEND_LESS_DATA_THEN_REQUESTED" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (6) download one piece from valid peer who send less data then requested every time and connected to me only later
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Given link to "SEND_LESS_DATA_THEN_REQUESTED" - fake-peer on port "4040" with the following pieces - with delay: "500" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (7) start download pieces which no one provide until new peers come with the missing pieces
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application download the following pieces - for torrent: "<torrent>":
      | 1 |
      | 0 |
    Given link to "SEND_LESS_DATA_THEN_REQUESTED" - fake-peer on port "4040" with the following pieces - with delay: "1000" milliseconds - for torrent: "<torrent>"
      | 0 |
    Given link to "SEND_LESS_DATA_THEN_REQUESTED" - fake-peer on port "4041" with the following pieces - with delay: "1000" milliseconds - for torrent: "<torrent>"
      | 1 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (7) download from two fake-peers which one of them respond too late every time
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Given link to "SEND_RESPOND_AFTER_TIMEOUT" - fake-peer on port "4040" with the following pieces - with delay: "400" milliseconds - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - with delay: "400" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  # This test won't pass because the PieceDownloader will not try to download from that peer again after timeout
  # but the PieceDownloader will try to download other pieces and then come back again to download from that peer again the same piece.
  # so I don't need this test.
#  Scenario Outline: (8) download from a fake-peer who respond too late every time
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given the following saved pieces - for torrent: "<torrent>": - none
#    Then application download the following pieces - for torrent: "<torrent>":
#      | 0 |
#    Given link to "SEND_RESPOND_AFTER_TIMEOUT" - fake-peer on port "4040" with the following pieces - with delay: "400" milliseconds - for torrent: "<torrent>"
#      | 0 |
#    Then wait until download is finished - for torrent: "<torrent>"
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test/   |