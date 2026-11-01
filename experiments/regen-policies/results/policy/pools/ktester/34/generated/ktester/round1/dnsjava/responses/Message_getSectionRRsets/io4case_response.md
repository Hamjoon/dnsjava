{
  "testCases": [
    {
      "group": "Empty but non‑null section list returns empty mutable list",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "empty mutable list (size 0) – not the singleton Collections.emptyList()",
          "description": "The internal sections[0] is an instantiated but empty ArrayList. The method should iterate over it, produce a new LinkedList, and return a mutable empty list rather than the immutable Collections.emptyList()."
        }
      ]
    },
    {
      "group": "Records with identical name/type/class but different RDATA are aggregated into a single RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "list containing 1 RRset; the RRset holds 3 Records (same name, type A, class IN, different IP addresses)",
          "description": "Populate Message.sections[1] with three A records that share name, type and class but have distinct address data. Since sameRRset() only checks name, type and class, all three should be added to the same RRset."
        }
      ]
    },
    {
      "group": "RRset order follows first occurrence of each distinct RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "list of 2 RRsets; first RRset contains records R1 and R2 (same RRset), second RRset contains records R3 and R4 (different RRset)",
          "description": "Insert four records into Message.sections[2] in the order: R1 (A), R2 (A, same name/type/class as R1), R3 (MX), R4 (MX, same as R3). The algorithm should create the first RRset for R1, add R2 to it, then create a second RRset for R3 and add R4 to it, preserving the order of first appearance."
        }
      ]
    },
    {
      "group": "Returned list is a fresh instance on each call (no caching)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "two separate list instances; modifications to the first call's result do not affect the second call",
          "description": "After the first invocation of getSectionRRsets(0) (with a non‑null list of records), add a dummy RRset to the returned list. Invoke the method again and verify that the new list does not contain the dummy RRset and is a different object from the first returned list."
        }
      ]
    },
    {
      "group": "Upper valid section index with null slot returns empty collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "empty list (Collections.emptyList())",
          "description": "When Message.sections[3] is null (the default state for the additional section) the method should return the immutable empty list, confirming that the null‑check works for the highest valid index."
        }
      ]
    }
  ]
}