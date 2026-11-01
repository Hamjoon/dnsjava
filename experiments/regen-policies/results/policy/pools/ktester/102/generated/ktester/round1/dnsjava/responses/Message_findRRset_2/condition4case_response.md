{
  "group": "Message.findRRset(Name,int) overall boolean logic",
  "cases": [
    {
      "input": [
        { "parameter": "name", "value": "example.com." },
        { "parameter": "type", "value": 1 }
      ],
      "expected": true,
      "description": "First section (ANSWER) returns true, causing short‑circuit evaluation; AUTHORITY and ADDITIONAL sections are not queried."
    },
    {
      "input": [
        { "parameter": "name", "value": "example.org." },
        { "parameter": "type", "value": 2 }
      ],
      "expected": true,
      "description": "ANSWER section returns false, AUTHORITY section returns true; short‑circuit stops before ADDITIONAL is queried."
    },
    {
      "input": [
        { "parameter": "name", "value": "example.net." },
        { "parameter": "type", "value": 3 }
      ],
      "expected": true,
      "description": "Both ANSWER and AUTHORITY sections return false, ADDITIONAL returns true; overall result is true."
    },
    {
      "input": [
        { "parameter": "name", "value": "example.edu." },
        { "parameter": "type", "value": 4 }
      ],
      "expected": false,
      "description": "All three sections return false; the method returns false."
    },
    {
      "input": [
        { "parameter": "name", "value": null },
        { "parameter": "type", "value": 1 }
      ],
      "expected": "NullPointerException",
      "description": "Passing a null Name should propagate a NullPointerException from the overloaded findRRset method."
    }
  ]
}