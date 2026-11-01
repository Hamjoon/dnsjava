{
    "testCases": [
        {
            "group": "Null name handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": null
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"name must not be null\"",
                    "description": "Verifies that findExactMatch throws an IllegalArgumentException when the supplied Name argument is null."
                }
            ]
        },
        {
            "group": "Invalid type handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "type",
                            "value": -1
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Type.check",
                    "description": "Ensures that an illegal DNS type value causes Type.check to throw an IllegalArgumentException, which propagates out of findExactMatch."
                }
            ]
        },
        {
            "group": "No matching RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "nonexistent.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": null,
                    "description": "When the zone does not contain an RRset for the given name/type, findExactMatch should return null."
                }
            ]
        },
        {
            "group": "Exact match found – copy semantics",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "host.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
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
                    "description": "Adds an RRset to the Zone, then calls findExactMatch with the same name/type. The method must return a new RRset instance (different object identity) that contains the same records as the original, proving the copy‑on‑read behaviour."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "host.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": {
                        "type": "RRset",
                        "properties": {
                            "name": "host.example.com.",
                            "type": 1,
                            "recordsCount": "same as original"
                        },
                        "objectIdentity": "different",
                        "originalUnchangedAfterModification": true
                    },
                    "description": "After obtaining the copy, mutate the original RRset (e.g., add a new Record). The returned copy must remain unchanged, confirming that a deep copy was created rather than a reference to the original."
                }
            ]
        }
    ]
}