{
  "group": "Message.getOPT() - OPT record retrieval from ADDITIONAL section",
  "cases": [
    {
      "input": [],
      "expected": "OPTRecord instance (the first OPTRecord in the ADDITIONAL section)",
      "description": "The ADDITIONAL section contains a single OPTRecord. getOPT() should iterate over the section, detect the instance of OPTRecord and return it."
    },
    {
      "input": [],
      "expected": "OPTRecord instance (the first OPTRecord among several records)",
      "description": "The ADDITIONAL section contains several records (e.g., ARecord, AAAARecord) followed by an OPTRecord and then other records. getOPT() must return the first encountered OPTRecord, ignoring preceding non‑OPT records."
    },
    {
      "input": [],
      "expected": "OPTRecord instance (the first of multiple OPTRecords)",
      "description": "The ADDITIONAL section contains two OPTRecord objects (perhaps with different UDP payload sizes). The method should stop at the first one and return it, never reaching the second."
    },
    {
      "input": [],
      "expected": "null",
      "description": "The ADDITIONAL section contains records, but none are instances of OPTRecord. After scanning the whole list, getOPT() should fall through the loop and return null."
    },
    {
      "input": [],
      "expected": "null",
      "description": "The ADDITIONAL section is empty (getSection(Section.ADDITIONAL) returns an empty list). The loop body never executes, and the method returns null."
    }
  ]
}