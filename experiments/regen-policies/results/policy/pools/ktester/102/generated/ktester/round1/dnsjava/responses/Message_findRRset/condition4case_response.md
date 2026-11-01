{
    "test_cases": [
        {
            "group": "Valid scenarios - section empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ANSWER"
                        }
                    ],
                    "expected": false,
                    "description": "The requested section exists but is null (no records). The method should return false without iterating."
                }
            ]
        },
        {
            "group": "Valid scenarios - no matching record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.org.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ANSWER"
                        }
                    ],
                    "expected": false,
                    "description": "Section contains records, but none match both the supplied name and type. The method should iterate all records and finally return false."
                }
            ]
        },
        {
            "group": "Valid scenarios - matching record found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.net.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.AAAA"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": true,
                    "description": "Section contains at least one record whose name and type exactly match the arguments. The method should return true as soon as the first match is found."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.net.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.AAAA"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": true,
                    "description": "Multiple records exist; the matching record is not the first element. The loop must continue until it finds the match and then return true."
                }
            ]
        },
        {
            "group": "Invalid inputs - type validation failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "-1"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.QUESTION"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Type.check throws IllegalArgumentException for an invalid type value. The method should not proceed to section handling."
                }
            ]
        },
        {
            "group": "Invalid inputs - section validation failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        },
                        {
                            "parameter": "section",
                            "value": "10"  , 
                            "note": "value outside the valid Section range (0‑3)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Section.check throws IllegalArgumentException for an out‑of‑range section index. The method should abort before accessing the sections array."
                }
            ]
        }
    ]
}