Feature: download pieces from fake-peers

  Scenario Outline: download 2 pieces ,one by one, from a valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0  |
      | -1 |
    When application download the following pieces - concurrent piece's downloads: "1" - for torrent: "<torrent>":
      | 0  |
      | -1 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then application downloaded the following pieces - for torrent: "<torrent>":
      | 0  |
      | -1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: download 2 pieces concurrently from a valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0  |
      | -1 |
    When application download the following pieces - concurrent piece's downloads: "2" - for torrent: "<torrent>":
      | 0  |
      | -1 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then application downloaded the following pieces - for torrent: "<torrent>":
      | 0  |
      | -1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: fail to download 2 pieces from a invalid fake-peer which have big delay in response.
    Given torrent: "<torrent>","<downloadLocation>"
    Given link to "RESPOND_WITH_DELAY_3000" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0  |
      | -1 |
    When application download the following pieces - concurrent piece's downloads: "1" - for torrent: "<torrent>":
      | 0  |
      | -1 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then application couldn't downloaded the following pieces - for torrent: "<torrent>":
      | 0  |
      | -1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: download 2 pieces ,one by one, from a valid fake-peer while we pause and resume the download.
    Given torrent: "<torrent>","<downloadLocation>"
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    When application download the following pieces - concurrent piece's downloads: "1" - for torrent: "<torrent>":
      | 0 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD |
    Then application couldn't downloaded the following pieces - for torrent: "<torrent>":
      | 0 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD |
    Then application downloaded the following pieces - for torrent: "<torrent>":
      | 0 |
    When fake-peer on port "4040" notify on more completed pieces using "BitFieldMessage" - for torrent: "<torrent>":
      | -1 |
    When application download the following pieces - concurrent piece's downloads: "1" - for torrent: "<torrent>":
      | -1 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD |
    Then application couldn't downloaded the following pieces - for torrent: "<torrent>":
      | -1 |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | RESUME_DOWNLOAD |
    Then application downloaded the following pieces - for torrent: "<torrent>":
      | -1 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |