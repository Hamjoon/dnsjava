{
    "test_cases": [
        {
            "group": "Null key array handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message with a simple query"
                        },
                        {
                            "parameter": "b",
                            "value": "null"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) thrown before any MAC processing",
                    "description": "The key byte array is null. generate should validate the key argument and fail fast."
                }
            ]
        },
        {
            "group": "Old TSIGRecord with null MAC",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a standard query"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x01,0x02,0x03,0x04}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with algorithm HMAC_SHA256, key byte[]{0x01,0x02,0x03,0x04}, and MAC set to null"
                        }
                    ],
                    "expected": "NullPointerException thrown when the previous MAC is accessed",
                    "description": "When a prior TSIGRecord is supplied but its MAC field is null, generate attempts to concatenate the old MAC and throws NPE."
                }
            ]
        },
        {
            "group": "Old TSIGRecord with null algorithm name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a TXT query"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x0A,0x0B,0x0C,0x0D}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord where getAlgorithm() returns null (algorithm name not set)"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) during algorithm mapping",
                    "description": "generate uses the algorithm name from the old TSIGRecord to initialise the Mac. A null algorithm should cause an immediate exception."
                }
            ]
        },
        {
            "group": "Invalid error code handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message with a single A query"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0xAA,0xBB,0xCC,0xDD}"
                        },
                        {
                            "parameter": "error",
                            "value": "-1"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because error code is outside the valid TSIG range (0‑255)",
                    "description": "generate should validate the error code parameter; a negative value is illegal and must be rejected."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message with a single A query"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0xAA,0xBB,0xCC,0xDD}"
                        },
                        {
                            "parameter": "error",
                            "value": "300"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because error code exceeds the 0‑255 range",
                    "description": "TSIG error codes are defined as unsigned 16‑bit values but the implementation restricts to the standard range; values >255 should be rejected."
                }
            ]
        },
        {
            "group": "Message serialization exception handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "MockMessage whose toWire() method throws IOException"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x55,0x66,0x77,0x88}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException (or a specific TSIGException) wrapping the IOException from Message.toWire()",
                    "description": "If the DNS Message cannot be converted to wire format, generate must propagate the failure as a runtime exception."
                }
            ]
        }
    ]
}