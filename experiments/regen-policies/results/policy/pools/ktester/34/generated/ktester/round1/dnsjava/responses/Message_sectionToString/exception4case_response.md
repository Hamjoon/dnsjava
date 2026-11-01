{
    "testCases": [
        {
            "group": "Invalid Sections - Integer.MAX_VALUE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "2147483647"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check should reject Integer.MAX_VALUE as it exceeds the defined section range and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Invalid Sections - Integer.MIN_VALUE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "-2147483648"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check should reject Integer.MIN_VALUE as it is below the allowed section range and throw IllegalArgumentException."
                }
            ]
        }
    ]
}