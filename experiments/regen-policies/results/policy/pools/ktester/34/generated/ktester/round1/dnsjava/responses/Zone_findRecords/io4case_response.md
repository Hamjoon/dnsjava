{
    "group": "Invalid type parameter",
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
            "description": "Calling findRecords with a negative DNS type should trigger Type.check and throw IllegalArgumentException before any zone lookup."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "example.com."
                },
                {
                    "parameter": "type",
                    "value": 65536
                }
            ],
            "expected": "IllegalArgumentException thrown by Type.check",
            "description": "Calling findRecords with a type value larger than the 16‑bit DNS type range should cause Type.check to throw IllegalArgumentException."
        }
    ]
}