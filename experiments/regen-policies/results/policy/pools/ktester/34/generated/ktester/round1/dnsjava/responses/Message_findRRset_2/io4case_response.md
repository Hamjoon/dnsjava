{
  "test_cases": [
    {
      "group": "findRRset short‑circuit behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "Record.Type.A"
            }
          ],
          "expected": "true",
          "description": "Message contains an A record for example.com. in both ANSWER and AUTHORITY sections. The first internal call (ANSWER) returns true, so the overall method returns true without invoking the AUTHORITY or ADDITIONAL checks."
        }
      ]
    },
    {
      "group": "findRRset falls‑through to later sections",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "Record.Type.NS"
            }
          ],
          "expected": "true",
          "description": "No NS record in ANSWER, but one exists in AUTHORITY. The method returns false after the first check, true after the second, and never evaluates ADDITIONAL."
        }
      ]
    },
    {
      "group": "findRRset with empty message",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "Record.Type.A"
            }
          ],
          "expected": "false",
          "description": "Message has no records in any section. All three internal findRRset calls return false, so the method returns false."
        }
      ]
    },
    {
      "group": "findRRset ANY type matches any record",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "Record.Type.ANY"
            }
          ],
          "expected": "true",
          "description": "Message contains an MX record for example.com. in the ANSWER section. Using the ANY type (255) should match any existing RRset for the name, causing the method to return true."
        }
      ]
    },
    {
      "group": "findRRset name case‑insensitivity",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "ExAmPlE.CoM."
            },
            {
              "parameter": "type",
              "value": "Record.Type.A"
            }
          ],
          "expected": "true",
          "description": "Message stores an A record with the canonical name \"example.com.\" in the ANSWER section. DNS names are case‑insensitive, so a mixed‑case query should still locate the RRset and return true."
        }
      ]
    },
    {
      "group": "findRRset out‑of‑range positive type",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "65536"
            }
          ],
          "expected": "false",
          "description": "A type value larger than the maximum 16‑bit DNS type is supplied. The internal findRRset implementation treats it as non‑matching, resulting in false for all sections."
        }
      ]
    },
    {
      "group": "findRRset subdomain mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "sub.example.com."
            },
            {
              "parameter": "type",
              "value": "Record.Type.A"
            }
          ],
          "expected": "false",
          "description": "Message contains an A record for the parent name \"example.com.\" but the query asks for the subdomain \"sub.example.com.\". No exact match exists in any section, so the method returns false."
        }
      ]
    }
  ]
}