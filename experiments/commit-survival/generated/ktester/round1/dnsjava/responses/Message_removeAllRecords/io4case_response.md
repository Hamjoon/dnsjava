{
  "testCases": [
    {
      "group": "Idempotent removal",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "sections[2] remains null and header.setCount is called with count 0 without throwing an exception",
          "description": "Calling removeAllRecords on a section that is already empty should be a no‑op but still set the header count to zero and not raise any exception."
        }
      ]
    },
    {
      "group": "Removal after adding records",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "sections[1] becomes null, header count for section 1 is set to 0, and previously added records are no longer accessible via getSection(1)",
          "description": "Add one or more records to the ANSWER section, verify that getSection(1) returns a non‑empty list, then call removeAllRecords(1) and confirm the list is cleared and the header count is reset."
        }
      ]
    },
    {
      "group": "Header count verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "header.setCount(0, 0) is invoked exactly once",
          "description": "Using a mock Header, ensure that removeAllRecords calls setCount with the correct section index and a count of zero."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "header.setCount(3, 0) is invoked exactly once",
          "description": "Same verification for the ADDITIONAL section."
        }
      ]
    },
    {
      "group": "Sections array null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "NullPointerException",
          "description": "If the internal `sections` array is deliberately set to null (e.g., via reflection) before invoking removeAllRecords, the method should throw a NullPointerException when attempting to assign `sections[section] = null`."
        }
      ]
    },
    {
      "group": "Header null after removal",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "NullPointerException",
          "description": "When the header field is null, removeAllRecords should still attempt to call `header.setCount` and therefore throw a NullPointerException, even if the sections entry is successfully cleared."
        }
      ]
    }
  ]
}