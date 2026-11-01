{
  "testCases": [
    {
      "group": "Valid Sections - Empty QUESTION",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "\"\"",
          "description": "A newly created Message with no records in the QUESTION section should produce an empty string when sectionToString is called."
        }
      ]
    },
    {
      "group": "Valid Sections - Multiple Same‑Type Records",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "\"example.com. IN A 1.2.3.4\\nexample.com. IN A 5.6.7.8\\n\"",
          "description": "Add two A records to the ANSWER section and verify that sectionToString returns both records, each on its own line, preserving insertion order."
        }
      ]
    },
    {
      "group": "Valid Sections - Record with TTL and MX",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": "\"example.com. 3600 IN MX 10 mail.example.com.\\n\"",
          "description": "Add an MX record with an explicit TTL to the AUTHORITY section and ensure the TTL, class, priority and target appear correctly in the string representation."
        }
      ]
    },
    {
      "group": "Valid Sections - Single OPT Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": "\"opt.example.com. IN OPT ; pseudo‑record\\n\"",
          "description": "Add only an OPT pseudo‑record to the ADDITIONAL section and verify that sectionToString returns the correct formatted line."
        }
      ]
    },
    {
      "group": "Valid Sections - Order Preservation",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "\"example.com. IN A 1.2.3.4\\nexample.com. IN AAAA 2001:db8::1\\nexample.com. IN CNAME alias.example.com.\\n\"",
          "description": "Add three different records (A, AAAA, CNAME) to the ANSWER section in a specific order and confirm that sectionToString outputs them in the exact same order."
        }
      ]
    },
    {
      "group": "Valid Sections - Removal Effect",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "\"\"",
          "description": "Add a record to the ANSWER section, remove it using removeRecord, and then call sectionToString; the result should be an empty string, confirming that removal is reflected."
        }
      ]
    }
  ]
}