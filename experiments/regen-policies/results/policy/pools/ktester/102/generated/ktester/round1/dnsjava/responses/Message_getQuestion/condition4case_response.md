{
  "testCases": [
    {
      "group": "Question section is null",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "When the QUESTION section array entry is null, getQuestion() should return null."
        }
      ]
    },
    {
      "group": "Question section is empty list",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "When the QUESTION section contains an empty List<Record>, getQuestion() should return null."
        }
      ]
    },
    {
      "group": "Question section contains a single record",
      "cases": [
        {
          "input": [],
          "expected": "recordA",
          "description": "After adding a single Record (recordA) to the QUESTION section via addRecord(..., Section.QUESTION), getQuestion() must return that same Record."
        }
      ]
    },
    {
      "group": "Question section contains multiple records",
      "cases": [
        {
          "input": [],
          "expected": "firstRecord",
          "description": "When multiple Records are added to the QUESTION section, getQuestion() should return the first Record that was added (index 0)."
        }
      ]
    },
    {
      "group": "Question section becomes empty after removal",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Add a Record to the QUESTION section, verify getQuestion() returns it, then remove all records from the QUESTION section using removeAllRecords(...). After removal, getQuestion() must return null."
        }
      ]
    }
  ]
}