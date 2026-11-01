{
    "group": "Null internal sections array",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 0
                }
            ],
            "expected": "NullPointerException",
            "description": "When the Message instance has its internal `sections` field set to null (e.g., via reflection or a faulty constructor), calling `getSection(0)` should throw a NullPointerException while trying to evaluate `sections[section]`."
        }
    ]
}