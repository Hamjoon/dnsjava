{
  "testCases": [
    {
      "group": "Valid Sections - Empty Section",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "\"\"",
          "description": "Calling sectionToString on a newly created Message where the ANSWER section contains no records should return an empty string."
        }
      ]
    },
    {
      "group": "Valid Sections - Single Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "\"example.com. IN A 1.2.3.4\\n\"",
          "description": "Add a single A record to the QUESTION section and verify that sectionToString returns the correctly formatted string representation of that record."
        }
      ]
    },
    {
      "group": "Valid Sections - Multiple Records",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": "\"opt.example.com. IN OPT ; pseudo‑record\\nns.example.com. IN NS ns1.example.com.\\n\"",
          "description": "Add two different records (an OPT pseudo‑record and an NS record) to the ADDITIONAL section and verify that sectionToString concatenates both records in the order they were added, each followed by a newline."
        }
      ]
    },
    {
      "group": "Invalid Sections - Negative Index",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "-1"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Section.check should reject a negative section index and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid Sections - Out‑of‑Range Index",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": "5"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Section.check should reject an index larger than the maximum defined section (e.g., 5) and throw IllegalArgumentException."
        }
      ]
    }
  ]
}