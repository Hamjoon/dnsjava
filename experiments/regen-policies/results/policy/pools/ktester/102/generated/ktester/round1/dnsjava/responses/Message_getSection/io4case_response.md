{
    "testCases": [
        {
            "group": "Invalid section indices",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": -1
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Section.check",
                    "description": "Pass a negative section index; the method should reject it and throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 4
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Section.check",
                    "description": "Pass an index equal to the number of defined sections (4); the method should reject it and throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 100
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Section.check",
                    "description": "Pass a far‑out‑of‑range index; the method should reject it and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Empty sections return empty immutable list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "empty unmodifiable list (size 0); attempts to modify throw UnsupportedOperationException",
                    "description": "No records have been added to the ANSWER section; getSection(0) should return an empty immutable list."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "empty unmodifiable list (size 0); attempts to modify throw UnsupportedOperationException",
                    "description": "No records have been added to the AUTHORITY section; getSection(1) should return an empty immutable list."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "empty unmodifiable list (size 0); attempts to modify throw UnsupportedOperationException",
                    "description": "No records have been added to the ADDITIONAL section; getSection(2) should return an empty immutable list."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": "empty unmodifiable list (size 0); attempts to modify throw UnsupportedOperationException",
                    "description": "No records have been added to the QUESTION section (if defined as index 3); getSection(3) should return an empty immutable list."
                }
            ]
        },
        {
            "group": "Unmodifiable list behavior",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "UnsupportedOperationException when calling add/remove on the returned list",
                    "description": "Add two dummy Record objects to the ADDITIONAL section (index 2). After calling getSection(2), attempt to add a third Record to the returned list; the operation must fail with UnsupportedOperationException, confirming the list is unmodifiable."
                }
            ]
        },
        {
            "group": "Section independence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "unmodifiable list containing exactly the two Records added to ANSWER section (size = 2)",
                    "description": "Add two Records to ANSWER (0) and one Record to AUTHORITY (1). Verify that getSection(0) returns only the two ANSWER records and does not include the AUTHORITY record."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "unmodifiable list containing exactly the one Record added to AUTHORITY section (size = 1)",
                    "description": "Using the same setup as the previous case, verify that getSection(1) returns only the AUTHORITY record and not the ANSWER records."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "empty unmodifiable list (size 0)",
                    "description": "No records were added to ADDITIONAL (2); ensure getSection(2) remains empty and independent of modifications in other sections."
                }
            ]
        }
    ]
}