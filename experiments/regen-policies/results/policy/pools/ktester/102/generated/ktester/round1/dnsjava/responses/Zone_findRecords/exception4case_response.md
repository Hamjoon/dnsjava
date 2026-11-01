{
    "test_cases": [
        {
            "group": "Origin null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Zone instance has a null `origin`. When `findRecords` calls `name.subdomain(origin)`, a NullPointerException is thrown because `origin` is null."
                }
            ]
        },
        {
            "group": "Type upper bound validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 65536
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Type.check(int)",
                    "description": "The DNS type value exceeds the valid 16‑bit range. `Type.check` should reject it and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Internal exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "IllegalStateException propagated from findRecordsWithoutLock",
                    "description": "A subclass or spy of `Zone` overrides `findRecordsWithoutLock` to throw an IllegalStateException. The exception must propagate out of `findRecords` (i.e., not be caught or transformed)."
                }
            ]
        }
    ]
}