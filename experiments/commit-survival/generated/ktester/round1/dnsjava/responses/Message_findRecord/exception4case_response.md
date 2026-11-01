{
    "group": "findRecord null sections array",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordX"
                },
                {
                    "parameter": "section",
                    "value": 0
                }
            ],
            "expected": "NullPointerException",
            "description": "The internal `sections` array is deliberately set to null (e.g., via reflection). Any access to `sections[section]` should throw a NullPointerException."
        }
    ]
}