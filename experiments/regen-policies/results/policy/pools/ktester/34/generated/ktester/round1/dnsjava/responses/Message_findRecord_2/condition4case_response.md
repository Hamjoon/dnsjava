{
  "group": "findRecord_success",
  "cases": [
    {
      "input": [
        {
          "parameter": "r",
          "value": "Record(name=example.com., type=A, data=1.2.3.4)"
        }
      ],
      "expected": "true",
      "description": "The record is added to the ANSWER section (Section.ANSWER). The method should iterate over sections, find the non‑null list that contains the record and return true."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "Record(name=example.org., type=MX, data=mail.example.org.)"
        }
      ],
      "expected": "true",
      "description": "The record is added to the AUTHORITY section (Section.AUTHORITY). The loop reaches this section, sees a non‑null list that contains the record and returns true."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "Record(name=example.net., type=TXT, data=\"test\")"
        }
      ],
      "expected": "true",
      "description": "The record is added to the ADDITIONAL section (Section.ADDITIONAL). The method should locate it and return true."
    }
  ]
}