{
  "testCases": [
    {
      "group": "OPTRecord in non‑ADDITIONAL section",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "An OPTRecord is added to the ANSWER section (or AUTHORITY) but the ADDITIONAL section remains empty; getOPT() must ignore records outside ADDITIONAL and return null."
        },
        {
          "input": [],
          "expected": "null",
          "description": "An OPTRecord is added to the QUESTION section (as a pseudo‑record) while ADDITIONAL contains only standard records; getOPT() should still return null."
        }
      ]
    },
    {
      "group": "Null entries in ADDITIONAL section",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "The ADDITIONAL section list contains a null element (simulating a malformed list) and no OPTRecord; the method should safely skip nulls and ultimately return null."
        },
        {
          "input": [],
          "expected": "instance of OPTRecord (the one after null)",
          "description": "A null entry precedes a valid OPTRecord in the ADDITIONAL list; getOPT() must continue iteration, ignore the null, and return the OPTRecord."
        }
      ]
    },
    {
      "group": "Large ADDITIONAL list with late OPTRecord",
      "cases": [
        {
          "input": [],
          "expected": "instance of OPTRecord (the late one)",
          "description": "The ADDITIONAL section contains 10,000 regular records followed by a single OPTRecord; getOPT() should iterate through the whole list and return the first (and only) OPTRecord found."
        }
      ]
    },
    {
      "group": "Multiple OPTRecords with later one earlier in list",
      "cases": [
        {
          "input": [],
          "expected": "instance of OPTRecord (the first encountered)",
          "description": "Two OPTRecord objects are added to ADDITIONAL, the first after a few non‑OPT records and the second near the end; getOPT() must return the first OPTRecord encountered and ignore the later one."
        }
      ]
    },
    {
      "group": "Empty ADDITIONAL section after removal",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Initially the ADDITIONAL section contains an OPTRecord, but removeAllRecords(Section.ADDITIONAL) is called before invoking getOPT(); the method should now return null."
        }
      ]
    }
  ]
}