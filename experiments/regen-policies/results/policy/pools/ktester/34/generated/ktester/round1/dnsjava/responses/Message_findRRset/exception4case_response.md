{
    "group": "Additional exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": 70000
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The supplied type exceeds the maximum DNS type value; Type.check should throw IllegalArgumentException before any array access."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": 10
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The supplied section index is larger than any defined DNS section; Section.check should throw IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "The internal `sections` array of the Message instance has been set to null (e.g., via reflection). Accessing `sections[section]` triggers a NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "A Record with a null name is added to the specified section. When the method evaluates `name.equals(r.getName())`, a NullPointerException is thrown because `r.getName()` returns null."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "The list for the given section contains a null element. The loop retrieves `null` as `r` and attempts to call `r.getType()`, resulting in a NullPointerException."
        }
    ]
}