{
    "test_cases": [
        {
            "group": "Null algorithm in TSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(null, \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Creating a TSIG with a null algorithm string should cause the TSIG constructor (and thus setTSIG) to throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Null secret in TSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", null)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Creating a TSIG with a null secret byte array must raise IllegalArgumentException because the secret cannot be null."
                }
            ]
        },
        {
            "group": "Invalid error code in three‑argument overload",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        },
                        {
                            "parameter": "error",
                            "value": "99999"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Passing an error code outside the valid Rcode range to setTSIG(TSIG, int, TSIGRecord) should result in IllegalArgumentException."
                }
            ]
        },
        {
            "group": "TSIG attached to another Message (illegal state)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If a TSIG instance has already been associated with a different Message (simulated by calling setTSIG on a first Message and then on a second Message without cloning), the second call should throw IllegalStateException because a TSIG cannot be reused across messages."
                }
            ]
        }
    ]
}