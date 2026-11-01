{
  "testCases": [
    {
      "group": "Valid section with no records (null list)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "Collections.emptyList() (an immutable empty list)",
          "description": "When the requested section index is valid but the internal `sections[section]` entry is null, `getSection` should return an empty immutable list."
        }
      ]
    },
    {
      "group": "Valid section with records (non‑null list)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "An unmodifiable list containing the added Record(s)",
          "description": "After adding one or more records to a valid section using `addRecord`, `getSection` must return a view that contains those records and is immutable (any attempt to modify it throws UnsupportedOperationException)."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "UnsupportedOperationException when trying to add a new element to the returned list",
          "description": "Verify that the list returned for a populated section is truly unmodifiable."
        }
      ]
    },
    {
      "group": "Invalid section index (out of range)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": -1
            }
          ],
          "expected": "IllegalArgumentException thrown by Section.check",
          "description": "Section indices less than the minimum allowed should cause `Section.check` to throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "IllegalArgumentException thrown by Section.check",
          "description": "Section indices greater than the maximum allowed should cause `Section.check` to throw an IllegalArgumentException."
        }
      ]
    }
  ]
}