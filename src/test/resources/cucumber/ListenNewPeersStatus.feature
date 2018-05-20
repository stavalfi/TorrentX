Feature: start/stop/restart listening to new peers and include side-effects in the test

  Scenario: (1) start listen
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "START_SEARCHING_PEERS_IN_PROGRESS":
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (1) start and resume listen
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESUME_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "START_SEARCHING_PEERS_IN_PROGRESS":
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (1) start and pause listen
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | PAUSE_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "START_SEARCHING_PEERS_IN_PROGRESS":
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (1) start and resume and restart listen
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESUME_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESTART_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "START_SEARCHING_PEERS_IN_PROGRESS":
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |

  Scenario: (1) start and (resume and restart and the same time) listen
    Given initial listen-status - default
    When listen-status is trying to change to:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to:
      | RESUME_LISTENING_IN_PROGRESS  |
      | RESTART_LISTENING_IN_PROGRESS |

    Then listen-status will change to: "START_SEARCHING_PEERS_IN_PROGRESS":
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |