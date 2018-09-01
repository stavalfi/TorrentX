Feature: connect to valid fake-peers and download a piece from them

  Scenario Outline: (1) download one piece from valid peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (2) download two pieces from valid peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (3) download one piece from valid peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (4) download multiple pieces from multiple valid peers
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
      | 0  |
      | 1  |
      | -1 |
    Then application download the following pieces - for torrent: "<torrent>":
      | 0  |
      | 1  |
      | -1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |