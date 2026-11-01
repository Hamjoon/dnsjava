{
  "testCases": [
    {
      "group": "Multiple OPTRecords present",
      "cases": [
        {
          "input": [],
          "expected": "first OPTRecord instance",
          "description": "The ADDITIONAL section contains several OPTRecord objects (e.g., three). getOPT() should return the first OPTRecord encountered and ignore the subsequent ones."
        }
      ]
    },
    {
      "group": "OPTRecord only in non‑ADDITIONAL section",
      "cases": [
        {
          "input": [],
          "expected": null,
          "description": "An OPTRecord is added to the ANSWER section (or any section other than ADDITIONAL). Since getOPT() scans only the ADDITIONAL section, it must return null."
        }
      ]
    },
    {
      "group": "Null entry in ADDITIONAL section",
      "cases": [
        {
          "input": [],
          "expected": null,
          "description": "The ADDITIONAL section list contains a null element followed by regular non‑OPT records. The instanceof check safely skips the null, and because no OPTRecord is present, getOPT() returns null."
        }
      ]
    },
    {
      "group": "OPTRecord removed after addition",
      "cases": [
        {
          "input": [],
          "expected": null,
          "description": "An OPTRecord is first added to the ADDITIONAL section and then removed using removeRecord(). After removal, getOPT() should no longer find an OPTRecord and must return null."
        }
      ]
    },
    {
      "group": "Additional section contains nulls and later OPTRecord",
      "cases": [
        {
          "input": [],
          "expected": "OPTRecord instance",
          "description": "The ADDITIONAL section includes a null entry, then some non‑OPT records, and finally an OPTRecord. getOPT() must ignore nulls and earlier non‑OPT records and return the OPTRecord."
        }
      ]
    }
  ]
}