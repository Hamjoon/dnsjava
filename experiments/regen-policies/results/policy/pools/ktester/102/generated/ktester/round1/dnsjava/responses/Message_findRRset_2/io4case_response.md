{
  "test_cases": [
    {
      "group": "Message.findRRset(Name,int) empty message handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": 1
            }
          ],
          "expected": false,
          "description": "The message contains no records in any section; the method must scan all three sections and return false."
        }
      ]
    },
    {
      "group": "Message.findRRset(Name,int) root name handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "."
            },
            {
              "parameter": "type",
              "value": 1
            }
          ],
          "expected": true,
          "description": "A record with the root name (\".\") and type A is added to the ANSWER section; findRRset should locate it and return true."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "."
            },
            {
              "parameter": "type",
              "value": 2
            }
          ],
          "expected": false,
          "description": "The root name exists only with type A; querying a different type (NS) must return false."
        }
      ]
    },
    {
      "group": "Message.findRRset(Name,int) case‑insensitivity of Name",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "ExAmPlE.CoM."
            },
            {
              "parameter": "type",
              "value": 1
            }
          ],
          "expected": true,
          "description": "A record is stored using the canonical lower‑case name \"example.com.\"; DNS names are case‑insensitive, so the mixed‑case query must still succeed."
        }
      ]
    },
    {
      "group": "Message.findRRset(Name,int) edge type values",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": -1
            }
          ],
          "expected": false,
          "description": "Negative type values are not valid DNS RR types; the implementation should safely return false rather than throwing."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": 0
            }
          ],
          "expected": false,
          "description": "Type 0 corresponds to the pseudo‑type \"ANY\" in some contexts but is not a concrete RR type; with no matching records the method must return false."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "example.com."
            },
            {
              "parameter": "type",
              "value": 65535
            }
          ],
          "expected": false,
          "description": "Maximum unsigned 16‑bit value is outside the range of defined RR types; the method should treat it as non‑existent and return false."
        }
      ]
    },
    {
      "group": "Message.findRRset(Name,int) wildcard name handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "sub.example.com."
            },
            {
              "parameter": "type",
              "value": 1
            }
          ],
          "expected": false,
          "description": "A record is stored with the wildcard name \"*.example.com.\". The findRRset method does not perform DNS wildcard expansion, therefore a direct lookup for \"sub.example.com.\" must return false."
        }
      ]
    }
  ]
}