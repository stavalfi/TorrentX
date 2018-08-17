Feature: connect to valid fake-peers and map between them and their pieces

  Scenario Outline: (1) get available pieces from one valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>":
      | 4040 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (2) get available pieces from two valid fake-peers
    Given torrent: "<torrent>","<downloadLocation>"
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>":
      | 4041 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (3) get available pieces from one non-valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>":
      | 4040 |
    When application request something from all fake-peer on port "4040" - for torrent: "<torrent>"
    Then application receive the following available pieces - for torrent: "<torrent>": - none
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>": - none
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>": - none

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (4) get available pieces from one non-valid fake-peer and valid fake-peer
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
      | 1 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>":
      | 4040 |
    When application request something from all fake-peer on port "4040" - for torrent: "<torrent>"
    Then application receive the following available pieces - for torrent: "<torrent>": - none
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>": - none
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 1 |
    Then application receive the following available fake-peers for piece: "1" - for torrent: "<torrent>":
      | 4041 |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (5) no fake-peers for specific piece and then a peer notify later that he have that piece
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>": - none
    Then application receive the following available pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (5) no fake-peers for specific piece and then a peer notify later that he have that piece
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>":
      | 1 |
      | 2 |
    Then application receive the following available pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 0 |
    Then application receive the following available pieces - for torrent: "<torrent>":
      | 0 |
    Then application receive the following available fake-peers for piece: "0" - for torrent: "<torrent>":
      | 4040 |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: (6) I can't find any pieces I don't have yet.
    Given torrent: "<torrent>","<downloadLocation>"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    Given the following saved pieces - for torrent: "<torrent>":
      | 1 |
      | 2 |
    Then application receive the following available pieces - for torrent: "<torrent>": - none
    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
      | 1 |
    Then application receive the following available pieces - for torrent: "<torrent>": - none

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |