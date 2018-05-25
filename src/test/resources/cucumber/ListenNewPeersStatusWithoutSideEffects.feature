Feature: start/stop/restart listening to new peers

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
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_SELF_RESOLVED |
    When listen-status is trying to change to - no side effects:
      | RESUME_LISTENING_WIND_UP |
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