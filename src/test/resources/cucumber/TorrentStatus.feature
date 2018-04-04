Feature: start/stop downloading/uploading

  Scenario Outline: start downloading & uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
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
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
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
      | PAUSE_UPLOAD |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: torrent-status is in downloading & uploading state and then stop uploading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | true  |
      | START_UPLOAD          | true  |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | true  |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | true  |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_UPLOAD |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_UPLOAD |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading and then stop uploading and downloading
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | true  |
      | START_UPLOAD          | true  |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | true  |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | true  |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading and then remove torrent
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | true  |
      | START_UPLOAD          | true  |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | true  |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | true  |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT |
    Then torrent-status for torrent "<torrent>" will be:
      | PAUSE_DOWNLOAD |
      | PAUSE_UPLOAD   |
      | REMOVE_TORRENT |

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when torrent is removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | true  |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: start downloading & uploading when torrent and files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | true  |
      | REMOVE_FILES          | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: complete downloading & uploading when torrent and files are removed
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | true  |
      | REMOVE_FILES          | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: starting download and upload which already download and upload already started
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | true  |
      | START_UPLOAD          | true  |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | true  |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | true  |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: starting a torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be:
      | START_DOWNLOAD  |
      | START_UPLOAD    |
      | RESUME_DOWNLOAD |
      | RESUME_UPLOAD   |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | START_DOWNLOAD |
      | START_UPLOAD   |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | true  |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_TORRENT |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: remove files twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | false |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | true  |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | REMOVE_FILES |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |

  Scenario Outline: complete torrent twice
    Given initial torrent-status for torrent: "<torrent>" in "<downloadLocation>" is:
      | START_DOWNLOAD        | false |
      | START_UPLOAD          | false |
      | PAUSE_DOWNLOAD        | false |
      | RESUME_DOWNLOAD       | false |
      | PAUSE_UPLOAD          | false |
      | RESUME_UPLOAD         | false |
      | COMPLETED_DOWNLOADING | true  |
      | REMOVE_TORRENT        | false |
      | REMOVE_FILES          | false |
    When torrent-status for torrent "<torrent>" is trying to change to:
      | COMPLETED_DOWNLOADING |
    Then torrent-status for torrent "<torrent>" will be: Empty-table

    Examples:
      | torrent                       | downloadLocation |
      | torrent-file-example1.torrent | torrents-test/   |