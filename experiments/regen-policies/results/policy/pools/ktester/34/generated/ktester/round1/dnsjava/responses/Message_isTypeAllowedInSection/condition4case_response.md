Invalid control character at: line 8 column 65 (char 190)
{
  "testCases": [
    {
      "group": "Allowed Types in AUTHORITY Section",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": 6 },               " // Type.SOA",
            { "parameter": "section", "value": 2 }             		] ,
          "expected": "true",
          "description": "Type SOA is allowed in the AUTHORITY section; method should return true."
        },
        {
          "input": [
            { "parameter": "type", "value": 2 },               " // Type.NS",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "true",
          "description": "Type NS is allowed in the AUTHORITY section; method should return true."
        },
        {
          "input": [
            { "parameter": "type", "value": 43 },              " // Type.DS",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "true",
          "description": "Type DS is allowed in the AUTHORITY section; method should return true."
        },
        {
          "input": [
            { "parameter": "type", "value": 47 },              " // Type.NSEC",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "true",
          "description": "Type NSEC is allowed in the AUTHORITY section; method should return true."
        },
        {
          "input": [
            { "parameter": "type", "value": 50 },              " // Type.NSEC3",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "true",
          "description": "Type NSEC3 is allowed in the AUTHORITY section; method should return true."
        }
      ]
    },
    {
      "group": "Allowed Types in ADDITIONAL Section",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": 1 },               " // Type.A",
            { "parameter": "section", "value": 3 } 
          ],
          "expected": "true",
          "description": "Type A is allowed in the ADDITIONAL section; method should return true."
        },
        {
          "input": [
            { "parameter": "type", "value": 28 },              " // Type.AAAA",
            { "parameter": "section", "value": 3 } 
          ],
          "expected": "true",
          "description": "Type AAAA is allowed in the ADDITIONAL section; method should return true."
        }
      ]
    },
    {
      "group": "Disallowed Types with Harden Property = true (default)",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": 1 },               " // Type.A",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "false",
          "description": "Type A is not allowed in AUTHORITY; with default hardening (true) the method returns false."
        },
        {
          "input": [
            { "parameter": "type", "value": 6 },               " // Type.SOA",
            { "parameter": "section", "value": 3 } 
          ],
          "expected": "false",
          "description": "Type SOA is not allowed in ADDITIONAL; with default hardening (true) the method returns false."
        },
        {
          "input": [
            { "parameter": "type", "value": 5 },               " // Type.CNAME (not in any allowed list)",
            { "parameter": "section", "value": 1 } 
          ],
          "expected": "false",
          "description": "CNAME in ANSWER section (neither AUTHORITY nor ADDITIONAL) falls through to default; hardening true makes result false."
        }
      ]
    },
    {
      "group": "Disallowed Types with Harden Property = false",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": 1 },               " // Type.A",
            { "parameter": "section", "value": 2 } 
          ],
          "expected": "true",
          "description": "Type A not allowed in AUTHORITY but system property set to false, so method returns true."
        },
        {
          "input": [
            { "parameter": "type", "value": 6 },               " // Type.SOA",
            { "parameter": "section", "value": 3 } 
          ],
          "expected": "true",
          "description": "Type SOA not allowed in ADDITIONAL but hardening disabled, method returns true."
        },
        {
          "input": [
            { "parameter": "type", "value": 5 },               " // Type.CNAME",
            { "parameter": "section", "value": 0 } 
          ],
          "expected": "true",
          "description": "CNAME in QUESTION section with hardening disabled; method returns true."
        }
      ]
    },
    {
      "group": "Invalid Type Parameter",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": -1 },
            { "parameter": "section", "value": 2 }
          ],
          "expected": "IllegalArgumentException",
          "description": "Negative type value triggers Type.check and throws IllegalArgumentException."
        },
        {
          "input": [
            { "parameter": "type", "value": 99999 },
            { "parameter": "section", "value": 3 }
          ],
          "expected": "IllegalArgumentException",
          "description": "Out‑of‑range type value triggers Type.check and throws IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid Section Parameter",
      "cases": [
        {
          "input": [
            { "parameter": "type", "value": 1 },
            { "parameter": "section", "value": -1 }
          ],
          "expected": "IllegalArgumentException",
          "description": "Negative section value triggers Section.check and throws IllegalArgumentException."
        },
        {
          "input": [
            { "parameter": "type", "value": 1 },
            { "parameter": "section", "value": 99 }
          ],
          "expected": "IllegalArgumentException",
          "description": "Undefined section value triggers Section.check and throws IllegalArgumentException."
        }
      ]
    }
  ]
}