{
    "group": "sectionToString_negativeSection_throwsException",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": -1
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Passing a negative section index should cause IllegalArgumentException because the internal helper expects a value between 0 and 3."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": -100
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "A large negative index also triggers the same validation, confirming that any i < 0 results in an exception."
        }
    ]
}