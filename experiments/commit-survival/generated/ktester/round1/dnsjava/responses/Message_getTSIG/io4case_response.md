{
  "testCases": [
    {
      "group": "Message.getTSIG - Non‑TSIG Last Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 2 and sections[ADDITIONAL] contains two Records: first a generic A Record, second a generic MX Record (type != TSIG)"
            }
          ],
          "expected": "null",
          "description": "When the ADDITIONAL section has records but the last one is not a TSIG record, getTSIG() must return null."
        }
      ]
    },
    {
      "group": "Message.getTSIG - TSIG Last Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 1 and sections[ADDITIONAL] contains a single TSIGRecord instance (created with new TSIGRecord(...))."
            }
          ],
          "expected": "instance of TSIGRecord (the same object stored in the section)",
          "description": "When the ADDITIONAL section contains exactly one record and it is a TSIGRecord, getTSIG() should return that TSIGRecord."
        },
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 3 and sections[ADDITIONAL] contains three records: two generic records followed by a TSIGRecord as the third (last) element."
            }
          ],
          "expected": "instance of TSIGRecord (the last element)",
          "description": "When multiple records exist and the last one is a TSIGRecord, getTSIG() must return that TSIGRecord."
        }
      ]
    },
    {
      "group": "Message.getTSIG - TSIG Not Last Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 2 and sections[ADDITIONAL] contains a TSIGRecord as the first element and a generic A Record as the second (last) element."
            }
          ],
          "expected": "null",
          "description": "Only the last record in the ADDITIONAL section is examined; a TSIGRecord that is not the last one must be ignored, resulting in null."
        }
      ]
    },
    {
      "group": "Message.getTSIG - Header Count Mismatch (IndexOutOfBounds)",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 5 but sections[ADDITIONAL] contains only 3 records."
            }
          ],
          "expected": "IndexOutOfBoundsException",
          "description": "If the header count is larger than the actual list size, getTSIG() will attempt to access an invalid index and throw IndexOutOfBoundsException."
        }
      ]
    },
    {
      "group": "Message.getTSIG - Null Additional Section",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message where header.getCount(ADDITIONAL) returns 1 but sections[ADDITIONAL] is null."
            }
          ],
          "expected": "NullPointerException",
          "description": "When the ADDITIONAL section array entry is null, attempting to retrieve the list causes a NullPointerException."
        }
      ]
    }
  ]
}