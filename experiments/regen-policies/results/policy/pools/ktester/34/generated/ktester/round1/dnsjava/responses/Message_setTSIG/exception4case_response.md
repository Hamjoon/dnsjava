{
    "test_cases": [
        {
            "group": "setTSIG_nullName",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(null, \"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or Message.tsigState becomes TSIG_FAILED) because the TSIG key name is null.",
                    "description": "A TSIG with a null Name should be rejected by setTSIG, resulting in an IllegalArgumentException (the library validates that the key name is non‑null)."
                }
            ]
        },
        {
            "group": "setTSIG_nonAbsoluteName",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"relative\"), \"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or tsigState == TSIG_FAILED) because the TSIG key name is not absolute (does not end with a trailing dot).",
                    "description": "The DNS specification requires TSIG key names to be fully qualified. Passing a relative name must cause setTSIG to reject the key."
                }
            ]
        },
        {
            "group": "setTSIG_nameTooLong",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(generateLongLabel(256) + \".example.\"), \"hmac-sha256\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or tsigState == TSIG_FAILED) because the key name exceeds the 255‑octet DNS name limit.",
                    "description": "Create a TSIG whose name label length is 256 characters, exceeding the DNS maximum. setTSIG should detect this and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "setTSIG_nullSecret",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"nullsecret.example.\"), \"hmac-md5\", null)"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or tsigState == TSIG_FAILED) because the secret byte array is null.",
                    "description": "A TSIG with a null secret is invalid; setTSIG must guard against a NullPointerException and instead raise an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "setTSIG_unsupportedAlgorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"badalg.example.\"), \"unsupported-algo\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or tsigState == TSIG_FAILED) because the algorithm string is not one of the supported HMAC algorithms.",
                    "description": "The library only supports a predefined set of HMAC algorithms. Supplying an unknown algorithm name should cause setTSIG to reject the key with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "setTSIG_overlyLargeSecret_causesFailureOnWire",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"bigsecret.example.\"), \"hmac-sha256\", new byte[70000])"
                        }
                    ],
                    "expected": "Message.tsigState becomes TSIG_FAILED (4) and Message.isSigned() returns false; no exception propagates from setTSIG itself.",
                    "description": "A secret larger than the maximum DNS message size should not cause setTSIG to throw, but the internal state must be marked as failed because the resulting message would exceed MAXLENGTH."
                }
            ]
        }
    ]
}