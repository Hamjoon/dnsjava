{
  "testCases": [
    {
      "group": "Valid section removal",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "sections[0] becomes null and header count for section 0 is set to 0",
          "description": "Remove all records from the first section (usually QUESTION). Verify that the internal sections array entry is cleared and that Header.setCount is called with count 0."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "sections[3] becomes null and header count for section 3 is set to 0",
          "description": "Remove all records from a higher valid index (e.g., ADDITIONAL). Ensure the same state changes as for section 0."
        }
      ]
    },
    {
      "group": "Invalid section index",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": -1
            }
          ],
          "expected": "ArrayIndexOutOfBoundsException",
          "description": "Passing a negative section index should cause the JVM to throw an ArrayIndexOutOfBoundsException because the sections array cannot be accessed with a negative index."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "ArrayIndexOutOfBoundsException",
          "description": "Passing an index larger than the sections array length (the array is of size 4) should also throw an ArrayIndexOutOfBoundsException."
        }
      ]
    },
    {
      "group": "Null header handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "NullPointerException",
          "description": "If the Message's header field is null, calling removeAllRecords should attempt to invoke header.setCount and therefore throw a NullPointerException."
        }
      ]
    }
  ]
}