{
  "testCases": [
    {
      "group": "sectionToString_multipleRecords",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "String containing both answer records in the order they were added, separated by a newline (e.g., \"example.com. 300 IN A 1.2.3.4\\nexample.com. 300 IN AAAA 2001:db8::1\")",
          "description": "Verify that sectionToString concatenates multiple records in the ANSWER section preserving insertion order."
        }
      ]
    },
    {
      "group": "sectionToString_variousRecordTypes",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": "String containing an MX record and a TXT record formatted correctly (e.g., \"example.com. 3600 IN MX 10 mail.example.com.\\nexample.com. 3600 IN TXT \\\"v=spf1 -all\\\"\")",
          "description": "Ensure that records of different DNS types are rendered properly when they share the same section."
        }
      ]
    },
    {
      "group": "sectionToString_largeSection",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": "String containing 100 A records, each on its own line; the total number of newline characters should be 99.",
          "description": "Stress test: verify that sectionToString can handle a large number of records without truncation or errors."
        }
      ]
    },
    {
      "group": "sectionToString_immutability",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "String representing the question record before the second addition; after adding another question record the previously returned string remains unchanged.",
          "description": "Check that the string returned by sectionToString is a snapshot and is not affected by later modifications to the message."
        }
      ]
    }
  ]
}