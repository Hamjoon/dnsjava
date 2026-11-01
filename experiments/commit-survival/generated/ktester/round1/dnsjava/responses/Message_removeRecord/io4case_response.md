{
  "tests": [
    {
      "group": "Message.removeRecord – empty section list",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=6, name=\"empty.com.\", type=A}"
            },
            {
              "parameter": "section",
              "value": "Message.ANSWER_SECTION (1)"
            }
          ],
          "expected": "false – the section list exists but is empty, List.remove returns false, header.decCount is not called and the method returns false",
          "description": "Ensures that an existing (non‑null) but empty List does not cause removal to succeed and that the header count remains unchanged."
        }
      ]
    },
    {
      "group": "Message.removeRecord – null record parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "null"
            },
            {
              "parameter": "section",
              "value": "Message.AUTHORITY_SECTION (2)"
            }
          ],
          "expected": "false – passing a null Record causes List.remove(null) to return false, header.decCount is not invoked and the method returns false",
          "description": "Verifies that the method safely handles a null Record argument without throwing an exception."
        }
      ]
    },
    {
      "group": "Message.removeRecord – duplicate records in section",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=7, name=\"dup.com.\", type=A}"
            },
            {
              "parameter": "section",
              "value": "Message.ADDITIONAL_SECTION (3)"
            }
          ],
          "expected": "true – one of the duplicate records is removed, the list size decreases by one, header.decCount is called exactly once and the method returns true",
          "description": "Checks that when a section contains two equal Record objects, removeRecord removes only the first occurrence, updates the header count once, and returns true."
        }
      ]
    },
    {
      "group": "Message.removeRecord – removal from QUESTION_SECTION",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=8, name=\"question.com.\", type=TXT}"
            },
            {
              "parameter": "section",
              "value": "Message.QUESTION_SECTION (0)"
            }
          ],
          "expected": "true – the question record is removed from the QUESTION_SECTION, header.decCount is invoked and the method returns true",
          "description": "Validates that removeRecord works for the QUESTION_SECTION (index 0), which is less commonly exercised."
        }
      ]
    },
    {
      "group": "Message.removeRecord – header is null leads to NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=9, name=\"noheader.com.\", type=MX}"
            },
            {
              "parameter": "section",
              "value": "Message.ANSWER_SECTION (1)"
            }
          ],
          "expected": "NullPointerException – after successful List.remove, header.decCount(section) is invoked on a null header, causing NPE",
          "description": "Ensures that the method propagates a NullPointerException when the Message's header field is null and a removal would otherwise succeed."
        }
      ]
    }
  ]
}