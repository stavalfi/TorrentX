Feature: (11) start/stop/restart listening to new peers

  Scenario: (1) start listener
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | START_LISTENING_IN_PROGRESS |
    Then listen-status will change to: "START_LISTENING_IN_PROGRESS" - no side effects:
      | START_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_WIND_UP     |

  Scenario: (2) resume listener
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
      | START_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_IN_PROGRESS  |
    When listen-status is trying to change "RESUME_LISTENING_WIND_UP" when it can and also - no side effects:
      | RESUME_LISTENING_SELF_RESOLVED |
    Then listen-status will change to: "RESUME_LISTENING_WIND_UP" - no side effects:
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |

  Scenario: (3) resume and pause listener
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
      | START_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_IN_PROGRESS  |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    Then listen-status will change to: "PAUSE_LISTENING_WIND_UP" - no side effects:
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (4) restart listener
    Given initial listen-status - no side effects:
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_IN_PROGRESS     |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP |
    Then listen-status will change to: "RESTART_LISTENING_WIND_UP" - no side effects:
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |

  Scenario: (5) restart listener while we resume
    Given initial listen-status - no side effects:
      | START_LISTENING_WIND_UP  |
      | RESUME_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_IN_PROGRESS     |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP |
    Then listen-status will change to: "RESTART_LISTENING_WIND_UP" - no side effects:
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |

  Scenario: (6) resume and then start to restart
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
      | START_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_WIND_UP      |
      | PAUSE_LISTENING_IN_PROGRESS   |
      | PAUSE_LISTENING_SELF_RESOLVED |
    Then listen-status will change to: "RESTART_LISTENING_IN_PROGRESS" - no side effects:
      | RESTART_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_WIND_UP       |
      | START_LISTENING_WIND_UP       |

  Scenario: (7) resume and then restart
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
      | START_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_IN_PROGRESS  |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_SELF_RESOLVED  |
      | RESTART_LISTENING_IN_PROGRESS  |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_WIND_UP      |
      | PAUSE_LISTENING_WIND_UP       |
      | PAUSE_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_SELF_RESOLVED |
      | RESTART_LISTENING_WIND_UP       |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP |
    Then listen-status will change to: "RESTART_LISTENING_WIND_UP" - no side effects:
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |

  Scenario: (8) resume and then start to restart
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP       |
      | START_LISTENING_WIND_UP       |
      | RESTART_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_IN_PROGRESS    |
      | RESTART_LISTENING_IN_PROGRESS  |
    Then listen-status will change to: "INITIALIZE" - no side effects:
      | PAUSE_LISTENING_WIND_UP       |
      | START_LISTENING_WIND_UP       |
      | RESTART_LISTENING_IN_PROGRESS |

  Scenario: (9) pause and restart
    Given initial listen-status - no side effects:
      | RESUME_LISTENING_WIND_UP |
      | START_LISTENING_WIND_UP  |
    # will change to RESTART_LISTENING_SELF_RESOLVED
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_IN_PROGRESS |
    # will change to PAUSE_LISTENING_IN_PROGRESS,RESTART_LISTENING_SELF_RESOLVED
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP       |
      | PAUSE_LISTENING_IN_PROGRESS     |
      | RESTART_LISTENING_SELF_RESOLVED |
    # will change to PAUSE_LISTENING_SELF_RESOLVED
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_SELF_RESOLVED |
    # will change to PAUSE_LISTENING_WIND_UP
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP       |
      | PAUSE_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_SELF_RESOLVED |
      | RESTART_LISTENING_IN_PROGRESS |
    # will change to RESTART_LISTENING_WIND_UP because everything is already set up
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP |
    # will change to ---nothing---
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP       |
      | RESTART_LISTENING_WIND_UP     |
      | RESTART_LISTENING_IN_PROGRESS |
    # will change to INITIALIZE
    When listen-status is trying to change to - no side effects:
      | PAUSE_LISTENING_WIND_UP   |
      | RESTART_LISTENING_WIND_UP |
      | INITIALIZE                |
    Then listen-status will change to: "INITIALIZE" - no side effects:
      | PAUSE_LISTENING_WIND_UP |
      | INITIALIZE              |

  Scenario: (10) resume by trigger
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP     |
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change "START_LISTENING_WIND_UP" when it can and also - no side effects:
      | START_LISTENING_SELF_RESOLVED |
    Then listen-status will change to: "START_LISTENING_WIND_UP" - no side effects:
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (11) resume by trigger and try to change to illegal states
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP     |
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change "START_LISTENING_WIND_UP" when it can and also - no side effects:
      | RESUME_LISTENING_WIND_UP      |
      | START_LISTENING_SELF_RESOLVED |
      | PAUSE_LISTENING_WIND_UP       |
    Then listen-status will change to: "START_LISTENING_WIND_UP" - no side effects:
      | START_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP |

  Scenario: (12) restart by trigger
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | START_LISTENING_IN_PROGRESS |
    When listen-status is trying to change "START_LISTENING_WIND_UP" when it can and also - no side effects:
      | START_LISTENING_SELF_RESOLVED |
      | RESTART_LISTENING_IN_PROGRESS |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | RESTART_LISTENING_WIND_UP |
    Then listen-status will change to: "RESTART_LISTENING_WIND_UP" - no side effects:
      | RESTART_LISTENING_WIND_UP |
      | PAUSE_LISTENING_WIND_UP   |

  Scenario: (13) when restart try to do anything but we will get ignored
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP   |
      | RESTART_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | START_LISTENING_IN_PROGRESS     |
      | START_LISTENING_SELF_RESOLVED   |
      | START_LISTENING_WIND_UP         |
      | RESUME_LISTENING_IN_PROGRESS    |
      | RESUME_LISTENING_SELF_RESOLVED  |
      | RESUME_LISTENING_WIND_UP        |
      | PAUSE_LISTENING_IN_PROGRESS     |
      | PAUSE_LISTENING_SELF_RESOLVED   |
      | PAUSE_LISTENING_WIND_UP         |
      | RESTART_LISTENING_IN_PROGRESS   |
      | RESTART_LISTENING_SELF_RESOLVED |
      | RESTART_LISTENING_WIND_UP       |
    Then listen-status will change to: "INITIALIZE" - no side effects:
      | PAUSE_LISTENING_WIND_UP   |
      | RESTART_LISTENING_WIND_UP |

  Scenario: (14) start listener
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | START_LISTENING_IN_PROGRESS |
    Then listen-status will change to: "START_LISTENING_IN_PROGRESS" - no side effects:
      | START_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_WIND_UP     |

  Scenario: (15) start listener
    Given initial listen-status - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    When listen-status is trying to change to - no side effects:
      | START_LISTENING_IN_PROGRESS |
    Then listen-status will change to: "START_LISTENING_IN_PROGRESS" - no side effects:
      | START_LISTENING_IN_PROGRESS |
      | PAUSE_LISTENING_WIND_UP     |

  Scenario: (16) get the first state without dispatching anything
    Given initial listen-status - without dispaching anything - no side effects:
      | PAUSE_LISTENING_WIND_UP |
    Then listen-status will change to: "INITIALIZE" - no side effects:
      | PAUSE_LISTENING_WIND_UP     |