Feature: inject incoming and outgoing messages to test the statistics

  Scenario: inject incoming messages and test the statistics
    Given incoming messages every "100" mill-seconds from a peer:
      | messageSize |
      | 100         |
      | 100         |
      | 100         |
    Then download statistics every 100 mill-seconds are from a peer:
      | downloadSpeed |
      | 100           |
      | 100           |
      | 100           |

    Given incoming messages every "100" mill-seconds from a peer:
      | messageSize |
      | 0           |
      | 100         |
      | 0           |
      | 0           |
    Then download statistics every 100 mill-seconds are from a peer:
      | downloadSpeed |
      | 0             |
      | 100           |
      | 0             |
      | 0             |

    Given incoming messages every "200" mill-seconds from a peer:
      | messageSize |
      | 0           |
      | 100         |
      | 0           |
      | 0           |
    Then download statistics every 0.1 mill-seconds are from a peer:
      | downloadSpeed |
      | 0             |
      | 0             |
      | 0             |
      | 100           |
      | 0             |
      | 0             |


  Scenario: inject outgoing messages and test the statistics
    Given outgoing messages every "100" mill-seconds from a peer:
      | messageSize |
      | 100         |
      | 100         |
      | 100         |
    Then upload statistics every 100 mill-seconds are from a peer:
      | downloadSpeed |
      | 100           |
      | 100           |
      | 100           |

    Given outgoing messages every "100" mill-seconds from a peer:
      | messageSize |
      | 0           |
      | 100         |
      | 0           |
      | 0           |
    Then upload statistics every 100 mill-seconds are from a peer:
      | downloadSpeed |
      | 0             |
      | 100           |
      | 0             |
      | 0             |

    Given outgoing messages every "200" mill-seconds from a peer:
      | messageSize |
      | 0           |
      | 100         |
      | 0           |
      | 0           |
    Then upload statistics every 0.1 mill-seconds are from a peer:
      | downloadSpeed |
      | 0             |
      | 0             |
      | 0             |
      | 100           |
      | 0             |
      | 0             |