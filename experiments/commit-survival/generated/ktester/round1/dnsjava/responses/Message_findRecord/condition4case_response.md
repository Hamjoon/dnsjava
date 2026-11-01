{
  "testCases": [
    {
      "group": "findRecord basic behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "recordA"
            },
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": false,
          "description": "The target section (0) has never been initialized, so `sections[0]` is null. The method should return false without throwing."
        },
        {
          "input": [
            {
              "parameter": "record",
              "value": "recordB"
            },
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": false,
          "description": "Section 1 is non‑null (a list was created by adding a different record), but it does not contain `recordB`. The method should evaluate the second operand to false and return false."
        },
        {
          "input": [
            {
              "parameter": "record",
              "value": "recordC"
            },
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": true,
          "description": "Section 2 contains `recordC` (added beforehand). Both conditions are true, so the method returns true."
        }
      ]
    },
    {
      "group": "findRecord edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "recordD"
            },
            {
              "parameter": "section",
              "value": -1
            }
          ],
          "expected": "ArrayIndexOutOfBoundsException",
          "description": "Negative section index leads to an out‑of‑bounds array access; the method should throw `ArrayIndexOutOfBoundsException`."
        },
        {
          "input": [
            {
              "parameter": "record",
              "value": "recordE"
            },
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "ArrayIndexOutOfBoundsException",
          "description": "Section index larger than the internal `sections` array size (which is 4 for standard DNS messages) triggers an out‑of‑bounds exception."
        },
        {
          "input": [
            {
              "parameter": "record",
              "value": null
            },
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": false,
          "description": "A null record is passed. The list for section 3 exists (created by adding another record) but `contains(null)` returns false, so the method returns false."
        }
      ]
    }
  ]
}