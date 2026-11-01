{
  "testCases": [
    {
      "group": "sectionToString_validSections",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "String containing the textual representation of the question record added to the message (e.g., \"example.com. IN A 1.2.3.4\")",
          "description": "Verify that sectionToString returns a non‑empty string for the QUESTION section when a single question record has been added."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "String containing the textual representation of the answer record(s) added to the message (e.g., \"example.com. 300 IN A 1.2.3.4\")",
          "description": "Verify that sectionToString correctly formats records stored in the ANSWER section."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": "String containing the textual representation of the authority record(s) added to the message (e.g., \"example.com. 300 IN NS ns1.example.com.\")",
          "description": "Verify that sectionToString correctly formats records stored in the AUTHORITY section."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": "String containing the textual representation of the additional record(s) added to the message (e.g., \"ns1.example.com. 300 IN A 5.6.7.8\")",
          "description": "Verify that sectionToString correctly formats records stored in the ADDITIONAL section."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "\"\" (empty string)",
          "description": "When the requested section contains no records, sectionToString should return an empty string."
        }
      ],
      "setup": "Create a Message instance. Use Message.addRecord(record, section) to add appropriate Record objects to each section before invoking sectionToString(section). For the empty‑section case, do not add any records."
    },
    {
      "group": "sectionToString_invalidSection",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "-1"
            }
          ],
          "expected": "IllegalArgumentException (or whatever exception Section.check throws) indicating an invalid section index",
          "description": "Section.check should reject a negative section index and the method should propagate the exception."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": "5"
            }
          ],
          "expected": "IllegalArgumentException (or whatever exception Section.check throws) indicating an invalid section index",
          "description": "Section.check should reject a section index larger than the maximum defined (typically 4) and the method should propagate the exception."
        }
      ],
      "setup": "Create a fresh Message instance (no records needed)."
    }
  ]
}