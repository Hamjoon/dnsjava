{
    "test_cases": [
        {
            "group": "findExactMatch_exceptions",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "65536"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Type.check(int) because type exceeds the maximum valid DNS type (65535)",
                    "description": "A type value greater than the highest defined DNS RR type should be rejected by Type.check, causing findExactMatch to throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Integer.MAX_VALUE"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Type.check(int) because type is out of the valid range",
                    "description": "Using Integer.MAX_VALUE as the type parameter tests the upper bound validation of Type.check; the method must propagate the IllegalArgumentException."
                }
            ]
        }
    ]
}