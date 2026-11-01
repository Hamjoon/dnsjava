{
    "groups": [
        {
            "group": "findRecords - invalid type values",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "0"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "DNS type 0 is reserved and not a valid query type; lookup should reject it and throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "70000"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Type values larger than the 16‑bit DNS type space (65535) are illegal; the method should throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findRecords - non‑absolute name handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"relative\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A Name that does not end with the root label (i.e., not absolute) is invalid for zone lookups; lookup should throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findRecords - empty name handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.empty()"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "An empty Name (zero labels) cannot be looked up in a zone; the method should reject it with IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findRecords - zone without origin",
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
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If a Zone instance is created without setting an origin (e.g., using the default constructor and never calling setOrigin), internal lookup will fail and should throw IllegalStateException."
                }
            ]
        }
    ]
}