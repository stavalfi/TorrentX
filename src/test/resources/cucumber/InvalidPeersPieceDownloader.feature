Feature: (9) connect to invalid fake-peers and download a piece from them

  Scenario Outline: (3) try to download from multiple invalid peers until valid peers are available
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Given link to "VALID_AND_SEND_CHOKE_AFTER_2_REQUESTS" - fake-peer on port "4040" with the following pieces - with delay: "200" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4041" with the following pieces - with delay: "400" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4042" with the following pieces - with delay: "800" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "VALID_AND_SEND_CHOKE_AFTER_2_REQUESTS" - fake-peer on port "4043" with the following pieces - with delay: "1000" milliseconds - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "VALID" - fake-peer on port "4044" with the following pieces - with delay: "1200" milliseconds - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4045" with the following pieces - with delay: "1400" milliseconds - for torrent: "<torrent>"
      | 1 |
    Then wait until download is finished - for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (1) download one piece from invalid peer who close the connection and then we download from other peer who came later
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - with delay: "500" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (2) download one piece from invalid peer who choke me and then we download from other peer who came later
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID_AND_SEND_CHOKE_AFTER_2_REQUESTS" - fake-peer on port "4040" with the following pieces - with delay: "0" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - with delay: "500" milliseconds - for torrent: "<torrent>"
      | 0 |
    Then wait until download is finished - for torrent: "<torrent>"

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |
