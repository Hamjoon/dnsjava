{
  "tests": [
    {
      "group": "removeRecord_invalidUpperBoundSection",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "mockRecordX"
            },
            {
              "parameter": "section",
              "value": "5"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Section index exceeds the maximum allowed value; Section.check should throw IllegalArgumentException and the method must not touch any internal state."
        }
      ]
    },
    {
      "group": "removeRecord_emptySectionList",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "mockRecordY"
            },
            {
              "parameter": "section",
              "value": "2"
            }
          ],
          "expected": false,
          "description": "The section list is non‑null but empty; remove returns false, so removeRecord should return false and header.decCount must not be invoked."
        }
      ]
    },
    {
      "group": "removeRecord_duplicateRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "duplicateRecord"
            },
            {
              "parameter": "section",
              "value": "1"
            }
          ],
          "expected": true,
          "description": "The section contains the same record twice; removeRecord should remove only the first occurrence, return true, and call header.decCount exactly once."
        }
      ]
    },
    {
      "group": "removeRecord_nullRecordInList",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": null
            },
            {
              "parameter": "section",
              "value": "3"
            }
          ],
          "expected": true,
          "description": "The section list contains a null entry and the caller passes null as the record; List.remove(null) succeeds, so removeRecord should return true and invoke header.decCount once."
        }
      ]
    },
    {
      "group": "removeRecord_immutableSectionList",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "mockRecordZ"
            },
            {
              "parameter": "section",
              "value": "0"
            }
          ],
          "expected": "UnsupportedOperationException",
          "description": "The section is backed by an immutable list (e.g., Collections.emptyList()); attempting to remove a record throws UnsupportedOperationException, which should propagate out of removeRecord."
        }
      ]
    }
  ]
}