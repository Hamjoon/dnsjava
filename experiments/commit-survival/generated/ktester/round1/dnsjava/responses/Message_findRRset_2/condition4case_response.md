{
  "testCases": [
    {
      "group": "Short‑circuit when first section matches",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "true",
          "description": "The overloaded findRRset(name, type, Section.ANSWER) is forced to return true. Because the logical OR operator is short‑circuiting, the method should return true without invoking the AUTHORITY or ADDITIONAL checks."
        }
      ]
    },
    {
      "group": "Short‑circuit when second section matches",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.org.\")" },
            { "parameter": "type", "value": "Type.MX" }
          ],
          "expected": "true",
          "description": "The ANSWER check returns false, the AUTHORITY check returns true, and the ADDITIONAL check must not be executed. The final result should be true."
        }
      ]
    },
    {
      "group": "Short‑circuit when third section matches",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.net.\")" },
            { "parameter": "type", "value": "Type.SRV" }
          ],
          "expected": "true",
          "description": "Both ANSWER and AUTHORITY checks return false, the ADDITIONAL check returns true. The method should return true after evaluating the third operand."
        }
      ]
    },
    {
      "group": "All sections return false",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"nonexistent.com.\")" },
            { "parameter": "type", "value": "Type.TXT" }
          ],
          "expected": "false",
          "description": "All three overloaded findRRset calls return false, so the combined OR expression yields false."
        }
      ]
    },
    {
      "group": "Null name argument",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "null" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null Name should cause a NullPointerException (the method forwards the call to the three‑argument version which dereferences the name)."
        }
      ]
    },
    {
      "group": "Invalid type argument (negative value)",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
            { "parameter": "type", "value": "-1" }
          ],
          "expected": "false",
          "description": "A type value that is not a valid DNS RR type should not match any RRset, therefore all three checks return false and the method returns false."
        }
      ]
    }
  ]
}