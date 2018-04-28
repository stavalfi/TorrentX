Feature: allocate and deallocate blocks from multiple threads

  Scenario: we allocate blocks from multiple threads successfully
    Given allocator for "10" blocks with "17_000" bytes each
    When the application allocate the following blocks from "3" threads:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |
    Then the allocator have the following free blocks:
      | 5 |
      | 6 |
      | 7 |
      | 8 |
      | 9 |
    Then the allocator have the following used blocks:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |

  Scenario: we allocate all blocks from multiple threads successfully
    Given allocator for "5" blocks with "17_000" bytes each
    When the application allocate the following blocks from "3" threads:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |
    Then the allocator have the following free blocks - none
    Then the allocator have the following used blocks:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |

  Scenario: we allocate blocks from single thread successfully
    Given allocator for "5" blocks with "17_000" bytes each
    When the application allocate the following blocks from "1" threads:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |
    Then the allocator have the following free blocks:
      | 5 |
      | 6 |
      | 7 |
      | 8 |
      | 9 |
    Then the allocator have the following used blocks:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |

  Scenario: we allocate and free blocks from multiple threads successfully
    Given allocator for "5" blocks with "17_000" bytes each
    When the application allocate the following blocks from "3" threads:
      | 0 |
      | 1 |
    Then the allocator have the following free blocks:
      | 2 |
      | 3 |
      | 4 |
    Then the allocator have the following used blocks:
      | 0 |
      | 1 |
    When the application free the following blocks:
      | 0 |
      | 1 |
    Then the allocator have the following free blocks:
      | 0 |
      | 1 |
      | 2 |
      | 3 |
      | 4 |
    Then the allocator have the following used blocks - none