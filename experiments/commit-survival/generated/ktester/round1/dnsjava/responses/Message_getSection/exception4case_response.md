{
    "test_cases": [
        {
            "group": "Message.getSection - index bounds validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": -1
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "Calling getSection with a negative index should throw ArrayIndexOutOfBoundsException because the internal sections array is accessed directly without validation."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 4
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "Calling getSection with an index equal to or greater than the length of the internal sections array (which is 4 in a standard Message) must throw ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "Message.getSection - internal null array handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the internal `sections` field is unexpectedly null (e.g., forced via reflection), any call to getSection should result in a NullPointerException when attempting to index the null array."
                }
            ]
        }
    ]
}