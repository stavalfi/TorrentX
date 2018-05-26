Feature: start/stop/restart listening to new peers and include side-effects in the test

#  Scenario: (1) start listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
#      | START_LISTENING_WIND_UP  |
#      | RESUME_LISTENING_WIND_UP |
#
#  Scenario: (11) start listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
#      | START_LISTENING_WIND_UP  |
#      | RESUME_LISTENING_WIND_UP |
#
#  Scenario: (12) start listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
#      | START_LISTENING_WIND_UP  |
#      | RESUME_LISTENING_WIND_UP |
#
#  Scenario: (2) start and then resume listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    When listen-status is trying to change to:
#      | RESUME_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "RESUME_LISTENING_WIND_UP":
#      | START_LISTENING_WIND_UP  |
#      | RESUME_LISTENING_WIND_UP |
#
#  Scenario: (3) start and then pause listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    When listen-status is trying to change to:
#      | PAUSE_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "PAUSE_LISTENING_WIND_UP":
#      | START_LISTENING_WIND_UP |
#      | PAUSE_LISTENING_WIND_UP |
#
#  Scenario: (4) start and then resume and restart listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    When listen-status is trying to change to:
#      | RESUME_LISTENING_IN_PROGRESS |
#    When listen-status is trying to change to:
#      | RESTART_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "INITIALIZE":
#      | PAUSE_LISTENING_WIND_UP |
#
#  Scenario: (5) start and then (resume and restart and the same time) listener
#    Given initial listen-status - default
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    When listen-status is trying to change to:
#      | RESUME_LISTENING_IN_PROGRESS  |
#      | RESTART_LISTENING_IN_PROGRESS |
#
#    Then listen-status will change to: "INITIALIZE":
#      | PAUSE_LISTENING_WIND_UP |