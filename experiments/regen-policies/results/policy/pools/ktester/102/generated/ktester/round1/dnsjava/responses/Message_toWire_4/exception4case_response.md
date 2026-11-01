{
    "new_test_cases": [
        {
            "group": "Insufficient positive maxLength without truncation (exception expected)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 5
                        },
                        {
                            "parameter": "truncate",
                            "value": false
                        }
                    ],
                    "expected": "MessageSizeExceededException is thrown with maxLength = 5",
                    "description": "A maxLength of 5 bytes is smaller than the DNS header size (12 bytes). The private toWire(out, 5) returns false, and because truncate is false the method must throw MessageSizeExceededException."
                }
            ]
        },
        {
            "group": "Insufficient positive maxLength with truncation flag true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 5
                        },
                        {
                            "parameter": "truncate",
                            "value": true
                        }
                    ],
                    "expected": "returns a non‑null byte[] of length 0 and no exception is thrown; internal size field is set to 0",
                    "description": "Even though the maxLength (5) cannot accommodate the DNS header, the truncate flag suppresses the exception. The method should return an empty byte array and record size = 0."
                }
            ]
        }
    ]
}