{
    "group": "Message.findRRset(Name, int, int) - additional edge cases",
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
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null `Name` should cause a NPE when the method evaluates `name.equals(r.getName())`."
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
                    "value": "-1"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException",
            "description": "A negative section index is outside the bounds of the internal `sections` array, leading to an AIOOBE on the first array access."
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
                    "value": "Message.Section.MAX + 1"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException",
            "description": "An index larger than the highest defined section triggers an AIOOBE when `sections[section]` is accessed."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"example.com.\")"
                },
                {
                    "parameter": "type",
                    "value": "Type.ANY"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": false,
            "description": "Even though a record with the requested name exists, its concrete type (e.g., A) does not equal `Type.ANY` (0), so the method must return false."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"example.com.\")"
                },
                {
                    "parameter": "type",
                    "value": "Type.ANY"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": true,
            "description": "A record of type `ANY` (0) with the matching name is present; the equality check succeeds and the method returns true."
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
            "description": "The section contains many records; the matching record appears after several non‑matching entries, confirming that the loop continues until a match is found."
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
            "expected": "NullPointerException",
            "description": "The section list contains a `null` element; when the loop reaches this element, `r.getType()` throws NPE."
        }
    ]
}