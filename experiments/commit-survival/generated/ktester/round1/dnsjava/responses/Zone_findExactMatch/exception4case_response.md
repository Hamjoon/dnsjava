{
    "test_cases": [
        {
            "group": "findExactMatch - type value exceeds DNS maximum (65535)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"www.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "70000"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The DNS type field is limited to 16 bits (0‑65535). Supplying a value larger than the maximum should cause the internal `oneRRset` validation to throw an IllegalArgumentException, which propagates out of `findExactMatch`."
                }
            ]
        },
        {
            "group": "findExactMatch - type value is Integer.MAX_VALUE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "2147483647"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Using an extreme integer for the type argument should be rejected by the method (or by `oneRRset`) as it is not a valid DNS RR type, resulting in an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findExactMatch - non‑absolute Name argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"relative\", false)"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The `Name` class requires an absolute (fully‑qualified) name for zone look‑ups. Passing a relative name should cause `exactName` (or an earlier validation step) to throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findExactMatch - internal state corruption triggers IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"www.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If the zone's internal data structures are corrupted (e.g., the map used by `exactName` is null), the private method may throw IllegalStateException. The test forces this condition (by mocking or reflection) and verifies that the exception propagates."
                }
            ]
        }
    ]
}