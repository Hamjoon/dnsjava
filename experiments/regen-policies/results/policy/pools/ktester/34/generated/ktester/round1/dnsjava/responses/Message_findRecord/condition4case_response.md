{
    "testCases": [
        {
            "group": "Valid section – empty list (null) → false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "RecordMock{id=1}"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.Section.ANSWER"
                        }
                    ],
                    "expected": false,
                    "description": "The requested section exists but its List<Record> is null. findRecord should return false without throwing."
                }
            ]
        },
        {
            "group": "Valid section – list present but does not contain record → false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "RecordMock{id=2}"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.Section.AUTHORITY"
                        }
                    ],
                    "expected": false,
                    "description": "The section list is non‑null but does not contain the supplied Record. The method must return false."
                }
            ]
        },
        {
            "group": "Valid section – list contains record → true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "RecordMock{id=3}"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.Section.ADDITIONAL"
                        }
                    ],
                    "expected": true,
                    "description": "The section list is non‑null and contains the supplied Record. The method must return true."
                }
            ]
        },
        {
            "group": "Invalid section (negative) → IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "RecordMock{id=4}"
                        },
                        {
                            "parameter": "section",
                            "value": "-1"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check should reject a negative section index and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Invalid section (out of range) → IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "RecordMock{id=5}"
                        },
                        {
                            "parameter": "section",
                            "value": "5"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check should reject an index larger than the maximum allowed (4) and throw IllegalArgumentException."
                }
            ]
        }
    ]
}