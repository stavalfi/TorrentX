Feature: inject incoming and outgoing messages to test the statistics

  Scenario: inject incoming messages and test the statistics
    Given size of incoming messages every "100" mill-seconds from a peer:
      | 100 |
      | 100 |
      | 100 |

    Then download statistics every 100 mill-seconds are from a peer:
      | 100 |
      | 100 |
      | 100 |

    Given size of incoming messages every "100" mill-seconds from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Then download statistics every 100 mill-seconds are from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Given size of incoming messages every "200" mill-seconds from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Then download statistics every 100 mill-seconds are from a peer:
      | 0   |
      | 0   |
      | 0   |
      | 100 |
      | 0   |
      | 0   |

  Scenario: inject outgoing messages and test the statistics
    Given size of outgoing messages every "100" mill-seconds from a peer:
      | 100 |
      | 100 |
      | 100 |

    Then upload statistics every 100 mill-seconds are from a peer:
      | 100 |
      | 100 |
      | 100 |

    Given size of outgoing messages every "100" mill-seconds from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Then upload statistics every 100 mill-seconds are from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Given size of outgoing messages every "200" mill-seconds from a peer:
      | 0   |
      | 100 |
      | 0   |
      | 0   |

    Then upload statistics every 100 mill-seconds are from a peer:
      | 0   |
      | 0   |
      | 0   |
      | 100 |
      | 0   |
      | 0   |