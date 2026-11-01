{
  "testCases": [
    {
      "group": "Valid scenarios - empty list section",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"example.org.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            },
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": false,
          "description": "The requested section exists and is an empty List (not null). The method should iterate zero times and return false."
        }
      ]
    },
    {
      "group": "Valid scenarios - match first record",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"first.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.MX"
            },
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": true,
          "description": "The first record in the section matches both name and type; the method should return true immediately without scanning the rest."
        }
      ]
    },
    {
      "group": "Valid scenarios - match last record",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"last.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.NS"
            },
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": true,
          "description": "Multiple records are present; only the last one matches the supplied name and type. The loop must scan all preceding records before returning true."
        }
      ]
    },
    {
      "group": "Valid scenarios - different type same name",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"mixed.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.SOA"
            },
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": false,
          "description": "The section contains records with the same name but different types. Because none have the requested type, the method should return false."
        }
      ]
    },
    {
      "group": "Valid scenarios - different name same type",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"nonexistent.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.TXT"
            },
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": false,
          "description": "Records of the requested type exist in the section, but none have the supplied name. The method must return false after full iteration."
        }
      ]
    },
    {
      "group": "Invalid inputs - null name",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "null"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            },
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null Name causes a NullPointerException when name.equals(...) is invoked. The method does not guard against null, so the exception is expected."
        }
      ]
    }
  ]
}