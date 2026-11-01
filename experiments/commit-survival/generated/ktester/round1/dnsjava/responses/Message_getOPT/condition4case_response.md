{
  "testCases": [
    {
      "group": "OPTRecord present - first record",
      "cases": [
        {
          "input": [],
          "expected": "instance of OPTRecord (the one added first)",
          "description": "Message contains an OPTRecord as the first element in the ADDITIONAL section; getOPT() should return that OPTRecord."
        }
      ]
    },
    {
      "group": "OPTRecord present - not first record",
      "cases": [
        {
          "input": [],
          "expected": "instance of OPTRecord (the first OPTRecord encountered in the list)",
          "description": "Message contains several non‑OPT records followed by an OPTRecord in the ADDITIONAL section; getOPT() should iterate through the list and return the OPTRecord when it is found."
        },
        {
          "input": [],
          "expected": "instance of OPTRecord (the first OPTRecord, even if multiple are present)",
          "description": "Message contains two OPTRecord objects in the ADDITIONAL section; getOPT() must return the first one encountered and ignore the second."
        }
      ]
    },
    {
      "group": "No OPTRecord in ADDITIONAL section",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Message's ADDITIONAL section contains only non‑OPT records; after the loop finishes getOPT() should return null."
        },
        {
          "input": [],
          "expected": "null",
          "description": "Message's ADDITIONAL section is empty; the method should immediately return null."
        }
      ]
    }
  ]
}