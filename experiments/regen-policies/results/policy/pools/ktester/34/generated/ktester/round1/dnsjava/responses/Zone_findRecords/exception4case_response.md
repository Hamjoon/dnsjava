{
    "group": "Null origin in Zone",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "sub.example.com."
                },
                {
                    "parameter": "type",
                    "value": 1
                }
            ],
            "expected": "NullPointerException",
            "description": "When the Zone instance has a null 'origin' field, the call to name.subdomain(origin) throws a NullPointerException. This test verifies that findRecords propagates the NPE for a zone with no origin."
        }
    ]
}