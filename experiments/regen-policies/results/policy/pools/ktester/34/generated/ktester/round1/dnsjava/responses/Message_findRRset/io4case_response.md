{
    "group": "Invalid inputs",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": -1
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The supplied type is not a valid DNS type; Type.check should throw IllegalArgumentException before any array access."
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
                    "value": -1
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The supplied section index is negative; Section.check should throw IllegalArgumentException."
        },
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
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null Name leads to a NullPointerException when name.equals(r.getName()) is evaluated."
        }
    ]
}