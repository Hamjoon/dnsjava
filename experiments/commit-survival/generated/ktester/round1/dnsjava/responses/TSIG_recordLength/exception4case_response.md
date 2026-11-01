{
    "test_cases": [
        {
            "group": "recordLength_exceptions",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "null"
                        },
                        {
                            "parameter": "alg",
                            "value": "HMAC_SHA256"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "TSIG instance created with a null `name` field. Calling recordLength() dereferences `name.length()` and must throw NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "alg",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "TSIG instance created with a null `alg` field. Calling recordLength() dereferences `alg.length()` and must throw NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "null"
                        },
                        {
                            "parameter": "alg",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "TSIG instance created with both `name` and `alg` set to null. Any dereference inside recordLength() triggers NullPointerException."
                }
            ]
        }
    ]
}