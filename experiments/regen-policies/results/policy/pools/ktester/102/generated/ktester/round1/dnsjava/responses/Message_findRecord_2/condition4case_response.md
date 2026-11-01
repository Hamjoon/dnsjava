{
  "groups": [
    {
      "group": "findRecord - record found in various sections",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=1, name=\"example.com.\", type=A}"
            }
          ],
          "expected": true,
          "description": "The target record is added to the ANSWER section. The loop checks Section.ANSWER first, finds a non‑null list containing the record, and returns true immediately."
        },
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=2, name=\"example.org.\", type=MX}"
            }
          ],
          "expected": true,
          "description": "The ANSWER section is null, the AUTHORITY section contains the target record. The method skips the null ANSWER, evaluates AUTHORITY, finds the record and returns true."
        },
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=3, name=\"example.net.\", type=TXT}"
            }
          ],
          "expected": true,
          "description": "Both ANSWER and AUTHORITY sections are either null or do not contain the record, but the ADDITIONAL section holds the record. The loop reaches ADDITIONAL, finds the record and returns true."
        },
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=4, name=\"example.edu.\", type=AAAA}"
            }
          ],
          "expected": true,
          "description": "The ANSWER section contains an empty list, the AUTHORITY section contains a non‑null list that does not contain the record, and the ADDITIONAL section contains the record. The method correctly continues past empty/non‑matching sections and finally returns true."
        }
      ]
    },
    {
      "group": "findRecord - record not found",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=5, name=\"missing.com.\", type=SRV}"
            }
          ],
          "expected": false,
          "description": "All three sections (ANSWER, AUTHORITY, ADDITIONAL) are null. The loop never enters the inner condition and returns false after completing all iterations."
        },
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=6, name=\"absent.org.\", type=NS}"
            }
          ],
          "expected": false,
          "description": "Each section is non‑null but contains lists that do not include the target record. The method checks each list, finds `contains(r)` false each time, and finally returns false."
        },
        {
          "input": [
            {
              "parameter": "r",
              "value": "Record{id=7, name=\"empty.net.\", type=CNAME}"
            }
          ],
          "expected": false,
          "description": "ANSWER section is an empty list, AUTHORITY section is null, ADDITIONAL section is an empty list. No `contains` check succeeds, so the method returns false."
        }
      ]
    }
  ]
}