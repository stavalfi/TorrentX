Feature: connect to a fake peers and communicate with them.
  1. the fake peers response with the same peer-message they received.
  2. the second response will be delayed in 2 seconds.
  3. the third response will cause the peer to shutdown the connection.

  Scenario: we send peer-messages and must receive the same peer-messages back.
  I don't PortMessage because this indicated I changed my port I'm listening to.
  It will be checked in different Scenario - this test is too hard, I won't do it.

    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType      | receiveMessageType   | errorSignalType |
      | BitFieldMessage      | BitFieldMessage      |                 |
      | CancelMessage        | CancelMessage        |                 |
      | ChokeMessage         | ChokeMessage         |                 |
      | HaveMessage          | HaveMessage          |                 |
      | InterestedMessage    | InterestedMessage    |                 |
      | IsAliveMessage       | IsAliveMessage       |                 |
      | NotInterestedMessage | NotInterestedMessage |                 |
      | PieceMessage         | PieceMessage         |                 |
      | RequestMessage       | RequestMessage       |                 |
      | UnchokeMessage       | UnchokeMessage       |                 |

  Scenario: we send 3 peer-messages and the connection must be closed by the rules of the fake peers.
    Then application send to [peer ip: "localhost", peer port: "8980"] and receive the following messages:
      | sendMessageType | receiveMessageType | errorSignalType              |
      | BitFieldMessage | BitFieldMessage    |                              |
      | BitFieldMessage | BitFieldMessage    |                              |
      | BitFieldMessage |                    | PeerDisconnectedExceptionOMG |
