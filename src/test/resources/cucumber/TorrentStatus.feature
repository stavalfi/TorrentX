Feature: start/stop downloading/uploading

  Scenario Outline: start downloading & uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | RESUME_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading and then stop uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | RESUME_UPLOAD   |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_UPLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | PAUSE_UPLOAD    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: torrent-status is in downloading & uploading state and then stop uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | true  |
      | RESUME_DOWNLOAD                    | true  |
      | RESUME_UPLOAD                      | true  |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_UPLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | PAUSE_UPLOAD    |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading and then stop uploading and downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | true  |
      | RESUME_DOWNLOAD                    | true  |
      | RESUME_UPLOAD                      | true  |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD |
      | START_UPLOAD   |
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading & listen for incoming peers and then remove torrent
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | true  |
      | RESUME_DOWNLOAD                    | true  |
      | RESUME_UPLOAD                      | true  |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | true  |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD                    |
      | START_UPLOAD                      |
      | PAUSE_DOWNLOAD                    |
      | PAUSE_UPLOAD                      |
      | REMOVE_TORRENT                    |
      | START_LISTENING_TO_INCOMING_PEERS |
      | PAUSE_LISTENING_TO_INCOMING_PEERS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when torrent is removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | true  |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_TORRENT                    |
      | PAUSE_DOWNLOAD                    |
      | PAUSE_UPLOAD                      |
      | START_LISTENING_TO_INCOMING_PEERS |
      | PAUSE_LISTENING_TO_INCOMING_PEERS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | true  |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_FILES   |
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when torrent and files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | true  |
      | REMOVE_FILES                       | true  |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_TORRENT |
      | REMOVE_FILES   |
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: complete downloading & uploading when torrent and files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | true  |
      | REMOVE_FILES                       | true  |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_TORRENT |
      | REMOVE_FILES   |
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: starting download and upload which already download and upload already started
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | true  |
      | RESUME_DOWNLOAD                    | true  |
      | RESUME_UPLOAD                      | true  |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | RESUME_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: starting a torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | true  |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD                     |
      | START_UPLOAD                       |
      | RESUME_DOWNLOAD                    |
      | RESUME_UPLOAD                      |
      | START_LISTENING_TO_INCOMING_PEERS  |
      | RESUME_LISTENING_TO_INCOMING_PEERS |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD                     |
      | START_UPLOAD                       |
      | RESUME_DOWNLOAD                    |
      | RESUME_UPLOAD                      |
      | START_LISTENING_TO_INCOMING_PEERS  |
      | RESUME_LISTENING_TO_INCOMING_PEERS |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | true  |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_TORRENT                    |
      | START_LISTENING_TO_INCOMING_PEERS |
      | PAUSE_LISTENING_TO_INCOMING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove files twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | true  |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_FILES |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: complete torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | true  |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD        |
      | COMPLETED_DOWNLOADING |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start listening for incoming peers
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_LISTENING_TO_INCOMING_PEERS |
    Then torrent-status for torrent "<torrent>" will be:
      | START_LISTENING_TO_INCOMING_PEERS  |
      | RESUME_LISTENING_TO_INCOMING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: stop listening for incoming peers
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | true  |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_LISTENING_TO_INCOMING_PEERS |
    Then torrent-status for torrent "<torrent>" will be:
      | START_LISTENING_TO_INCOMING_PEERS |
      | PAUSE_LISTENING_TO_INCOMING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: stop listening for incoming peers while keep searching peers
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | true  |
      | RESUME_LISTENING_TO_INCOMING_PEERS | true  |
      | START_SEARCHING_PEERS              | true  |
      | RESUME_SEARCHING_PEERS             | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_LISTENING_TO_INCOMING_PEERS |
    Then torrent-status for torrent "<torrent>" will be:
      | START_LISTENING_TO_INCOMING_PEERS |
      | PAUSE_LISTENING_TO_INCOMING_PEERS |
      | START_SEARCHING_PEERS             |
      | RESUME_SEARCHING_PEERS            |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove torrent while searching peers
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | true  |
      | RESUME_SEARCHING_PEERS             | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_TORRENT        |
      | START_SEARCHING_PEERS |
      | PAUSE_SEARCHING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove files while searching peers
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | false |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | true  |
      | RESUME_SEARCHING_PEERS             | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES |
    Then torrent-status for torrent "<torrent>" will be:
      | REMOVE_FILES          |
      | START_SEARCHING_PEERS |
      | PAUSE_SEARCHING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start search peers when the download is already completed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | false |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | true  |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD        |
      | COMPLETED_DOWNLOADING |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start search peers while downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD                     | true  |
      | START_UPLOAD                       | false |
      | RESUME_DOWNLOAD                    | true  |
      | RESUME_UPLOAD                      | false |
      | COMPLETED_DOWNLOADING              | false |
      | REMOVE_TORRENT                     | false |
      | REMOVE_FILES                       | false |
      | START_LISTENING_TO_INCOMING_PEERS  | false |
      | RESUME_LISTENING_TO_INCOMING_PEERS | false |
      | START_SEARCHING_PEERS              | false |
      | RESUME_SEARCHING_PEERS             | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_SEARCHING_PEERS |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD         |
      | RESUME_DOWNLOAD        |
      | START_SEARCHING_PEERS  |
      | RESUME_SEARCHING_PEERS |
    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |