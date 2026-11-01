{
  "testCases": [
    {
      "group": "Message.removeRecord – sections array is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=10, name=\"nullsections.com.\", type=A}"
            },
            {
              "parameter": "section",
              "value": "Message.ANSWER_SECTION (1)"
            }
          ],
          "expected": "NullPointerException",
          "description": "The internal `sections` array of the Message instance is deliberately set to null. Accessing `sections[section]` triggers a NullPointerException before any null‑check or removal logic is executed."
        }
      ]
    },
    {
      "group": "Message.removeRecord – unmodifiable section list",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=11, name=\"unmod.com.\", type=TXT}"
            },
            {
              "parameter": "section",
              "value": "Message.ADDITIONAL_SECTION (3)"
            }
          ],
          "expected": "UnsupportedOperationException",
          "description": "The target section contains an unmodifiable list (e.g., `Collections.unmodifiableList`). When `removeRecord` attempts `sections[section].remove(r)`, the list throws `UnsupportedOperationException`. The method should not catch it, so the exception propagates."
        }
      ]
    },
    {
      "group": "Message.removeRecord – header.decCount throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "Record{id=12, name=\"badheader.com.\", type=MX}"
            },
            {
              "parameter": "section",
              "value": "Message.AUTHORITY_SECTION (2)"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A mock `Header` object is injected whose `decCount(int)` method deliberately throws `IllegalArgumentException`. After a successful `remove` (the record exists in the list), `removeRecord` calls `header.decCount(section)`, which propagates the exception."
        }
      ]
    }
  ]
}