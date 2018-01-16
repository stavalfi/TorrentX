Feature: connect to a fake peers and communicate with them.
  1. the fake peers response with the same peer-message they received.
  2. the second response will be delayed in 2 seconds.
  3. the third response will cause the peer to shutdown the connection.
  4. if application send PortMessage then the fake peer will send PortMessage with it's same port to my new port

  Scenario Outline: we send peer-messages and must receive the same peer-messages back.
  I don't PortMessage because this indicated I changed my port I'm listening to.
  It will be checked in different Scenario.
    Given new torrent file: "<torrentFilePath>" containing the following fake peers:
      | peerIp    | peerPort |
      | localhost | 8980     |
      | localhost | 8981     |
      | localhost | 8982     |
      | localhost | 8983     |
      | localhost | 8984     |
      | localhost | 8985     |
      | localhost | 8986     |
      | localhost | 8987     |
      | localhost | 8988     |
      | localhost | 8989     |
      | localhost | 8990     |

    Then application send Handshake request to the following peers:
      | peerIp    | peerPort |
      | localhost | 8980     |
      | localhost | 8981     |
      | localhost | 8982     |
      | localhost | 8983     |
      | localhost | 8984     |
      | localhost | 8985     |
      | localhost | 8986     |
      | localhost | 8987     |
      | localhost | 8988     |
      | localhost | 8989     |
      | localhost | 8990     |

    Then application receive Handshake response from the following peers:
      | peerIp    | peerPort | responseType |
      | localhost | 8980     | success      |
      | localhost | 8981     | success      |
      | localhost | 8982     | success      |
      | localhost | 8983     | success      |
      | localhost | 8984     | success      |
      | localhost | 8985     | success      |
      | localhost | 8986     | success      |
      | localhost | 8987     | success      |
      | localhost | 8988     | success      |
      | localhost | 8989     | success      |
      | localhost | 8990     | success      |

    Then application send in parallel to the following peers:
      | peerIp    | peerPort | peerMessageType      |
      | localhost | 8980     | BitFieldMessage      |
      | localhost | 8981     | CancelMessage        |
      | localhost | 8982     | ChokeMessage         |
      | localhost | 8983     | HaveMessage          |
      | localhost | 8984     | InterestedMessage    |
      | localhost | 8985     | IsAliveMessage       |
      | localhost | 8986     | NotInterestedMessage |
      | localhost | 8987     | PieceMessage         |
      | localhost | 8989     | RequestMessage       |
      | localhost | 8990     | UnchokeMessage       |

    Then application receive messages from the following peers:
      | peerIp    | peerPort | peerMessageType      |
      | localhost | 8980     | BitFieldMessage      |
      | localhost | 8981     | CancelMessage        |
      | localhost | 8982     | ChokeMessage         |
      | localhost | 8983     | HaveMessage          |
      | localhost | 8984     | InterestedMessage    |
      | localhost | 8985     | IsAliveMessage       |
      | localhost | 8986     | NotInterestedMessage |
      | localhost | 8987     | PieceMessage         |
      | localhost | 8989     | RequestMessage       |
      | localhost | 8990     | UnchokeMessage       |

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |

  Scenario Outline: we send 3 peer-messages and the connection must be closed by the rules of the fake peers.
    Given new torrent file: "<torrentFilePath>" containing the following fake peers:
      | peerIp    | peerPort |
      | localhost | 8980     |

    Then application send Handshake request to the following peers:
      | peerIp    | peerPort |
      | localhost | 8980     |

    Then application receive Handshake response from the following peers:
      | peerIp    | peerPort | responseType |
      | localhost | 8980     | success      |

    Then application send in parallel to the following peers:
      | peerIp    | peerPort | peerMessageType |
      | localhost | 8980     | BitFieldMessage |
      | localhost | 8980     | BitFieldMessage |
      | localhost | 8980     | BitFieldMessage |

    Then application receive messages from the following peers:
      | peerIp    | peerPort | peerMessageType               |
      | localhost | 8980     | BitFieldMessage               |
      | localhost | 8980     | BitFieldMessage               |
      | localhost | 8980     | PeerDisconnectedExceptionOMG! |

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |

  Scenario Outline: we change our port and the fake peers response to the new port.
    Given new torrent file: "<torrentFilePath>" containing the following fake peers:
      | peerIp    | peerPort |
      | localhost | 8980     |

    Then application send Handshake request to the following peers:
      | peerIp    | peerPort |
      | localhost | 8980     |

    Then application receive Handshake response from the following peers:
      | peerIp    | peerPort | responseType |
      | localhost | 8980     | success      |

    Then application send in parallel to the following peers:
      | peerIp    | peerPort | peerMessageType |
      | localhost | 8980     | PortMessage     |

    Then application receive messages from the following peers:
      | peerIp    | peerPort | peerMessageType |
      | localhost | 8980     | PortMessage     |

    Examples:
      | torrentFilePath               |
      | torrent-file-example1.torrent |
