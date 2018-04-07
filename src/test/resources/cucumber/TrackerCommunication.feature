Feature: test tracker api calls

  Background: read torrent file
    Given new torrent file: "tor.torrent"

  Scenario: find any tracker, from all the trackers, which response to: connect,announce and scrape requests
    Then application send and receive the following messages from a random tracker:
      | trackerRequestType | errorSignalType |
      | Connect            |                 |
      | Announce           |                 |
      | Scrape             |                 |

  Scenario: communicating with collection of trackers which contain a not-responding trackers
    Given additional not-responding trackers to the tracker-list
    Then application send and receive the following messages from a random tracker:
      | trackerRequestType | errorSignalType |
      | Connect            |                 |

  Scenario: communicating with collection of trackers which contain invalid urls of trackers
    Given only one invalid url of a tracker
    Then application send and receive the following messages from a random tracker:
      | trackerRequestType | errorSignalType |
      | Connect            |                 |




