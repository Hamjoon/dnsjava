{
    "testCases": [
        {
            "group": "Valid section removal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": {
                        "sectionArrayNull": true,
                        "headerCountZero": true
                    },
                    "description": "Remove all records from the QUESTION section (section 0). After the call, the internal sections array entry for index 0 must be null and the header count for that section must be reset to 0."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": {
                        "sectionArrayNull": true,
                        "headerCountZero": true
                    },
                    "description": "Remove all records from the ANSWER section (section 1). Verify that the section entry is cleared and the header count is set to 0."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": {
                        "sectionArrayNull": true,
                        "headerCountZero": true
                    },
                    "description": "Remove all records from the AUTHORITY section (section 2). Ensure proper nulling of the array element and header count reset."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": {
                        "sectionArrayNull": true,
                        "headerCountZero": true
                    },
                    "description": "Remove all records from the ADDITIONAL section (section 3). Confirm that the method clears the section and updates the header count."
                }
            ]
        },
        {
            "group": "Invalid section handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": -1
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Pass a negative section index. Section.check should reject the value and the method must throw an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 4
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Pass an index greater than the highest valid section (assuming sections are 0‑3). Section.check should throw an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 99
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Pass a far‑out‑of‑range index. The method must still rely on Section.check to raise an IllegalArgumentException."
                }
            ]
        }
    ]
}