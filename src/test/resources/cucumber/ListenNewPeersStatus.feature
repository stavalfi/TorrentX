Feature: (10) start/stop/restart listening to new peers and include side-effects in the test

  Scenario: (1) start listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (2) start and then resume listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESUME_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (3) start and then pause listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | PAUSE_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "PAUSE_LISTENING_WIND_UP":
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (4) start and then resume and restart listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESUME_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESTART_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "INITIALIZE":
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (5) start and then (resume and restart and the same time) listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    # TODO: there is a bug here which cause blocking
    When listen-status is trying to change to:
      | RESTART_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "INITIALIZE":
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (6) start listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (7) start listener
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (8) get the first state without dispatching anything
    Given initial listen-status - without dispaching anything - default
    Then listen-status will change to: "INITIALIZE":
      | PAUSE_LISTENING_WIND_UP |

    # TODO: check if this test doesn't work only in university
  Scenario Outline: (9) fake-peers connect to me
    Given initial listen-status - default
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" with default initial state
    When fake-peer on port "8050" try to connect for torrent "<torrent>", he receive the following error: "TimeoutException"
    When fake-peer on port "8051" try to connect for torrent "<torrent>", he receive the following error: "TimeoutException"
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When fake-peer on port "8050" try to connect for torrent "<torrent>", he succeed
    When fake-peer on port "8051" try to connect for torrent "<torrent>", he succeed
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test    |