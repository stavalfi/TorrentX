Feature: allocate and free blocks from multiple threads

  Scenario Outline: we create a piece-message with valid parameters
    Given allocator for "5" blocks with "17000" bytes each
    When we create the following piece-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -2         | 10   | 3      |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -2         | 10   | 3      |
    Then the allocator have "4" used blocks
    Then the allocator have "1" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we create a piece-message with invalid parameters
    Given allocator for "9" blocks with "17000" bytes each
    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 30   | -1     |
      | 2          |      | 3      |
      | -1         |      | 3      |
      # Note: I assume here that (allocatedBlockLength -1) + 3 <= pieceLength
      | -2         | -1   | 3      |
      | -3         |      |        |
      # Note: I assume here that (allocatedBlockLength -1) + allocatedBlockLength <= pieceLength
      | -4         | -1   | -1     |
      # Note: I assume here that (allocatedBlockLength -1) + allocatedBlockLength <= pieceLength
      | -5         | -1   |        |
      | -6         |      | -1     |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | -1     |
      | 1          | 30   | -1     |
      | 2          |      | 1      |
      | -1         |      | 1      |
      | -2         | -1   | 3      |
      | -3         |      | 1      |
      | -4         | -1   | -1     |
      | -5         | -1   | -1     |
      | -6         |      | 1      |
    Then the allocator have "9" used blocks
    Then the allocator have "0" free blocks

    Examples:
      | torrent                       |
      # don't replace this torrent. I'm counting on the piece length of each piece.
      | torrent-file-example1.torrent |

  Scenario Outline: we create a piece-message with valid parameters while the allocated-block-length is bigger than the last piece
    Given allocator for "1" blocks with allocated-block-length which is bigger than piece: "-1" for torrent: "<torrent>":
    When we create the following piece-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | -1         | 0    | 3      |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | -1         | 0    | 3      |
    Then the allocator have "1" used blocks
    Then the allocator have "0" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we create a piece-message with valid parameters while the allocated-block-length is bigger than the first piece
    # piece length = 524288 , last piece length = 65536
    Given allocator for "9" blocks with "524289" bytes each

    # last piece length = 65536
    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -1         | 524288 | 3      |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from  | length |
      | -1         | 65535 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 524288 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 524288 |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 1          | 30   | 524288 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 1          | 30   | 524258 |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | 2          | 524288 | 3      |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | 2          | 524287 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -2         | 524289 | 3      |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -2         | 524287 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -3         | 524288 | 524288 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -3         | 524287 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -4         | 524289 | 524289 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -4         | 524287 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -5         | 524289 | 524288 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -5         | 524287 | 1      |

    When we create the following piece-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -6         | 524288 | 524289 |
    Then we created the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from   | length |
      | -6         | 524287 | 1      |

    Then the allocator have "9" used blocks
    Then the allocator have "0" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |


  Scenario Outline: we create a request-message with valid parameters
    Given allocator for "5" blocks with "17000" bytes each
    When we create the following request-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -1         | 10   | 3      |
    Then we created the following request-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -1         | 10   | 3      |
    Then the allocator have "0" used blocks
    Then the allocator have "5" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we create a request-message with invalid parameters
    Given allocator for "8" blocks with "17000" bytes each
    When we create the following request-messages from "1" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    |        |
      | 1          | 30   | -1     |
      | 2          |      | 3      |
      | -1         |      | 3      |
      # Note: I assume here that (allocatedBlockLength -1) + 3 <= pieceLength
      | -2         | -1   | 3      |
      | -3         |      |        |
      # Note: I assume here that (allocatedBlockLength -1) + allocatedBlockLength <= pieceLength
      | -4         | -1   | -1     |
      # Note: I assume here that (allocatedBlockLength -1) + allocatedBlockLength <= pieceLength
      | -5         | -1   |        |
      | -6         |      | -1     |
    Then we created the following request-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | -1     |
      | 1          | 30   | -1     |
      | 2          |      | 1      |
      | -1         |      | 1      |
      | -2         | -1   | 3      |
      | -3         |      | 1      |
      | -4         | -1   | -1     |
      | -5         | -1   | -1     |
      | -6         |      | 1      |
    Then the allocator have "0" used blocks
    Then the allocator have "8" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we create a piece-message with valid parameters and free the allocations
    Given allocator for "5" blocks with "17000" bytes each
    When we create the following piece-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -2         | 10   | 3      |
    Then the allocator have "4" used blocks
    Then the allocator have "1" free blocks
    When we free the following piece-messages for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
    Then the allocator have "2" used blocks
    Then the allocator have "3" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we don't create any piece-message and free all the allocations
    Given allocator for "5" blocks with "17000" bytes each
    When we create the following piece-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
    Then the allocator have "0" used blocks
    Then the allocator have "5" free blocks
    When we free all piece-messages for torrent: "<torrent>"
    Then the allocator have "0" used blocks
    Then the allocator have "5" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |

  Scenario Outline: we create a piece-message with valid parameters and free all the allocations
    Given allocator for "5" blocks with "17000" bytes each
    When we create the following piece-messages from "5" threads for torrent: "<torrent>":
      | pieceIndex | from | length |
      | 0          | 0    | 1      |
      | 1          | 30   | 2      |
      | -1         | 0    | 3      |
      | -2         | 10   | 3      |
    Then the allocator have "4" used blocks
    Then the allocator have "1" free blocks
    When we free all piece-messages for torrent: "<torrent>"
    Then the allocator have "0" used blocks
    Then the allocator have "5" free blocks

    Examples:
      | torrent                       |
      | torrent-file-example1.torrent |