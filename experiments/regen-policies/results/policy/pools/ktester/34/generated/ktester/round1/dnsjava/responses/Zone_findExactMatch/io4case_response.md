{
    "testCases": [
        {
            "group": "Exact match with distinct Name instance",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "host.example.com." },
                        { "parameter": "type", "value": 1 }
                    ],
                    "expected": {
                        "type": "RRset",
                        "properties": {
                            "name": "host.example.com.",
                            "type": 1,
                            "recordsCount": "same as original"
                        },
                        "objectIdentity": "different"
                    },
                    "description": "Adds an RRset to the zone, creates a new Name object with the same textual representation, calls findExactMatch and verifies that the returned RRset is a copy (different object) but has identical contents."
                }
            ]
        },
        {
            "group": "Exact match with case‑insensitive Name",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "HOST.Example.Com." },
                        { "parameter": "type", "value": 1 }
                    ],
                    "expected": {
                        "type": "RRset",
                        "properties": {
                            "name": "host.example.com.",
                            "type": 1,
                            "recordsCount": "same as original"
                        },
                        "objectIdentity": "different"
                    },
                    "description": "Ensures DNS name matching is case‑insensitive: an RRset added under lower‑case name is retrieved using an upper‑case variant."
                }
            ]
        },
        {
            "group": "Exact match when multiple types exist for same name",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "multi.example.com." },
                        { "parameter": "type", "value": 15 }   ,
                        { "parameter": "type", "value": 1 }
                    ],
                    "expected": [
                        {
                            "type": "RRset",
                            "properties": {
                                "name": "multi.example.com.",
                                "type": 15,
                                "recordsCount": "same as original for type 15"
                            },
                            "objectIdentity": "different"
                        },
                        {
                            "type": "RRset",
                            "properties": {
                                "name": "multi.example.com.",
                                "type": 1,
                                "recordsCount": "same as original for type A"
                            },
                            "objectIdentity": "different"
                        }
                    ],
                    "description": "Adds two RRsets with the same name but different types (e.g., MX=15 and A=1). Calls findExactMatch for each type and verifies that the correct RRset is returned for each, demonstrating that the method does not confuse different types."
                }
            ]
        },
        {
            "group": "No match when name exists but type differs",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "host.example.com." },
                        { "parameter": "type", "value": 15 }
                    ],
                    "expected": null,
                    "description": "An RRset for name \"host.example.com.\" with type A (1) is present, but the query asks for MX (15). findExactMatch must return null because the exact type is not stored."
                }
            ]
        },
        {
            "group": "Exact match after removal returns null",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "temp.example.com." },
                        { "parameter": "type", "value": 1 }
                    ],
                    "expected": null,
                    "description": "Adds an RRset, verifies findExactMatch returns a copy, removes the RRset with removeRRset, then calls findExactMatch again and expects null."
                }
            ]
        },
        {
            "group": "Exact match with empty RRset",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "empty.example.com." },
                        { "parameter": "type", "value": 1 }
                    ],
                    "expected": {
                        "type": "RRset",
                        "properties": {
                            "name": "empty.example.com.",
                            "type": 1,
                            "recordsCount": 0
                        },
                        "objectIdentity": "different"
                    },
                    "description": "Adds an RRset that contains no records (empty RRset). findExactMatch should still return a new RRset instance with zero records."
                }
            ]
        },
        {
            "group": "Maximum valid DNS type value",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "maxtype.example.com." },
                        { "parameter": "type", "value": 65535 }
                    ],
                    "expected": null,
                    "description": "Uses the highest possible DNS type value (65535). Assuming the zone does not contain such a type, findExactMatch must return null and must not throw an exception, confirming that Type.check accepts the upper bound."
                }
            ]
        },
        {
            "group": "Boundary type value just above valid range",
            "cases": [
                {
                    "input": [
                        { "parameter": "name", "value": "boundary.example.com." },
                        { "parameter": "type", "value": 65536 }
                    ],
                    "expected": "IllegalArgumentException thrown by Type.check",
                    "description": "Ensures that a type value just beyond the valid DNS type range triggers Type.check to throw IllegalArgumentException."
                }
            ]
        }
    ]
}