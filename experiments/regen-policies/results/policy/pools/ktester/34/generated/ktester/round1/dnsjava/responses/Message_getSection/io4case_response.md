{
  "testCases": [
    {
      "group": "Valid section after removing all records (non‑null empty list)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "An unmodifiable empty list (not the shared Collections.emptyList())",
          "description": "Add one or more records to section 2, call removeAllRecords(2) so the internal list becomes an empty List (not null), then verify that getSection(2) returns an immutable empty list distinct from Collections.emptyList()."
        }
      ]
    },
    {
      "group": "Valid section with multiple records preserves order",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "An unmodifiable list containing the added records in the exact insertion order",
          "description": "Add three distinct Record objects to section 1 using addRecord in order R1, R2, R3. getSection(1) must return a list whose iterator yields R1, then R2, then R3, confirming order preservation."
        }
      ]
    },
    {
      "group": "Returned list reflects underlying modifications",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "The previously obtained list now includes the newly added record",
          "description": "Call getSection(3) when the internal list is empty (null → empty list). Then add a Record to section 3 via addRecord. The list reference obtained earlier should now contain that Record because Collections.unmodifiableList is a view of the underlying mutable list."
        }
      ]
    },
    {
      "group": "Valid maximum section index with null internal list",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "Collections.emptyList() (immutable empty list)",
          "description": "Assuming the maximum valid section index is 3, and sections[3] is still null, getSection(3) must return the shared immutable empty list."
        }
      ]
    },
    {
      "group": "Immutable list throws UnsupportedOperationException on remove",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "UnsupportedOperationException when invoking remove on the returned list",
          "description": "After adding at least one Record to section 1, retrieve the list via getSection(1) and attempt list.remove(0). The operation must fail with UnsupportedOperationException, confirming the list is truly unmodifiable."
        }
      ]
    }
  ]
}