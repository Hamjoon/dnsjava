{
  "testCases": [
    {
      "group": "findRRset basic scenarios",
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
          "description": "Message contains an A record for example.com. in the ANSWER section. The first call to findRRset(name, type, Section.ANSWER) returns true, so the overall method returns true without checking other sections."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.org."
            },
            {
              "parameter": "type",
              "value": "Record.Type.MX"
            }
          ],
          "expected": "true",
          "description": "No MX record for example.org. in ANSWER, but one exists in the AUTHORITY section. The first call returns false, the second call (AUTHORITY) returns true, causing the overall result to be true."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.net."
            },
            {
              "parameter": "type",
              "value": "Record.Type.TXT"
            }
          ],
          "expected": "true",
          "description": "The TXT record for example.net. is absent from ANSWER and AUTHORITY sections but present in the ADDITIONAL section. The method returns true after the third call succeeds."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "nonexistent.example."
            },
            {
              "parameter": "type",
              "value": "Record.Type.SOA"
            }
          ],
          "expected": "false",
          "description": "The queried name/type combination does not exist in any of the three sections. All three internal findRRset calls return false, so the method returns false."
        }
      ]
    },
    {
      "group": "findRRset edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": null
            },
            {
              "parameter": "type",
              "value": "Record.Type.A"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null Name should cause a NullPointerException (or the method’s internal validation to throw). The test verifies that the exception is thrown."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": "-1"
            }
          ],
          "expected": "false",
          "description": "An invalid DNS type (negative value) is supplied. The internal findRRset implementation treats it as not matching any record, so all three section checks return false and the method returns false."
        }
      ]
    }
  ]
}