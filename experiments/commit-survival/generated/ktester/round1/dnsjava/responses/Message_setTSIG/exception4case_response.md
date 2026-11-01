{
    "test_cases": [
        {
            "group": "Unsupported TSIG algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"example.com.\"), \"unsupported-algo\", \"dGVzdFNlY3JldA==\")"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "Calling setTSIG with a TSIG whose algorithm string is not recognized by the library should cause an IllegalArgumentException. This tests the algorithm validation branch inside setTSIG."
                }
            ]
        },
        {
            "group": "Duplicate TSIG record already present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"dup.example.com.\"), \"hmac-sha256\", \"dGVzdFNlY3JldA==\")"
                        }
                    ],
                    "expected": "IllegalStateException thrown",
                    "description": "If a TSIGRecord is already present in the message's additional section (added via addRecord), a subsequent call to setTSIG should reject the operation and throw IllegalStateException, preventing multiple TSIGs."
                }
            ]
        },
        {
            "group": "Null TSIG key after message already contains TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "Even when a TSIGRecord already exists in the message, passing a null key to setTSIG must still trigger the null‑check and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "TSIG key with null name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(null, \"hmac-sha256\", \"dGVzdFNlY3JldA==\")"
                        }
                    ],
                    "expected": "NullPointerException thrown",
                    "description": "Constructing a TSIG with a null Name is illegal; setTSIG should propagate the NullPointerException thrown by the TSIG constructor."
                }
            ]
        }
    ]
}