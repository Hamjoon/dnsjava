{
  "group": "Message.findRRset(Name, int, int) - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "name",
          "value": "Name.fromString(\"example.com.\")"
        },
        {
          "parameter": "type",
          "value": "Type.A"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": false,
      "description": "The requested section array entry is null. The method should hit the first `if (sections[section] == null)` branch and return false."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "Name.fromString(\"example.com.\")"
        },
        {
          "parameter": "type",
          "value": "Type.A"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": false,
      "description": "The section exists but contains an empty list. The loop executes zero times and the method falls through to the final `return false`."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "Name.fromString(\"example.com.\")"
        },
        {
          "parameter": "type",
          "value": "Type.A"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": false,
      "description": "The section contains records, but none have both the requested type and name. The loop iterates over all records, never satisfying the `if ((r.getType() == type) && name.equals(r.getName()))` condition, and the method returns false."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "Name.fromString(\"example.com.\")"
        },
        {
          "parameter": "type",
          "value": "Type.A"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": true,
      "description": "The first record in the section matches both the requested name and type. The method enters the loop, the condition is true on the first iteration, and it returns true immediately."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "Name.fromString(\"example.com.\")"
        },
        {
          "parameter": "type",
          "value": "Type.A"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": true,
      "description": "The matching record is not the first element (e.g., second or later). The loop iterates past non‑matching records, eventually satisfies the condition, and returns true."
    }
  ]
}