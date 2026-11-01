{
  "group": "addRRset(RRset) - Null handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "rrset",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "Calling addRRset with a null argument should throw a NullPointerException because the method immediately dereferences the argument to obtain its name."
    },
    {
      "input": [
        {
          "parameter": "rrset",
          "value": "RRset{name=null, type=A, records=[ARecord] }"
        }
      ],
      "expected": "NullPointerException",
      "description": "If the RRset's name is null, the internal call to addRRset(Name,RRset) will receive a null Name and should result in a NullPointerException."
    }
  ]
}