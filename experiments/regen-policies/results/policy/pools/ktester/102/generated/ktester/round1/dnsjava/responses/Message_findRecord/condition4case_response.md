{
    "testCases": [
        {
            "group": "Valid section – empty list (null) → record not found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ANSWER"
                        }
                    ],
                    "expected": "false",
                    "description": "The requested section exists but its internal list is null (no records have been added). The method should return false."
                }
            ]
        },
        {
            "group": "Valid section – non‑null list without the record → false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"missing.com.\"), DClass.IN, 3600, InetAddress.getByName(\"5.6.7.8\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": "false",
                    "description": "The section contains a list of records, but the list does not contain the supplied record. The method should return false."
                }
            ]
        },
        {
            "group": "Valid section – list contains the record → true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "record"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ADDITIONAL"
                        }
                    ],
                    "expected": "true",
                    "description": "The section contains a non‑null list and the supplied record is present in that list. The method should return true."
                }
            ]
        },
        {
            "group": "Valid section – null record argument → false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "null"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.QUESTION"
                        }
                    ],
                    "expected": "false",
                    "description": "A null record is passed. The section list is non‑null but does not contain null, so the method should return false (no exception is thrown)."
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
                            "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
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
                            "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "10"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check should reject a section index larger than the maximum allowed value and throw IllegalArgumentException."
                }
            ]
        }
    ]
}