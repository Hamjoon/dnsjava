{
    "groups": [
        {
            "group": "Null RRset name – expect NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset mock: getName() returns null, type=Type.A, size=1"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the RRset's name is null, the method attempts to call name.equals(origin) or name.subdomain(origin) which results in a NullPointerException. The test verifies that the exception is thrown before any lock acquisition."
                }
            ]
        },
        {
            "group": "Zone with null origin – expect NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset mock: type=Type.A, name=Name(\"www.example.com.\"), size=1"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If a Zone instance is constructed (or manipulated via reflection) with its origin field set to null, any call to addRRset will trigger a NullPointerException when evaluating name.subdomain(origin) or when checking SOA owner equality. The test ensures the method does not mask this condition."
                }
            ]
        }
    ]
}