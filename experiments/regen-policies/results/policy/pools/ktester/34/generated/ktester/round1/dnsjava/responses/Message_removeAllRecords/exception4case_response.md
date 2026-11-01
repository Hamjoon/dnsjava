{
    "groups": [
        {
            "group": "Null header handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Message instance has its header field set to null (e.g., via reflection). Calling removeAllRecords should attempt header.setCount and throw a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "With a null header and a valid section index, the method must still raise a NullPointerException when trying to update the header count."
                }
            ]
        },
        {
            "group": "Null sections array handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The internal sections array is null (e.g., set via reflection). removeAllRecords should attempt to assign to sections[section] and throw a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When sections is null and a valid section index is provided, the method must fail with a NullPointerException before reaching header.setCount."
                }
            ]
        }
    ]
}