{
    "tests": [
        {
            "group": "Valid TSIG key handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"example.com.\"), \"hmac-sha256\", \"dGVzdFNlY3JldA==\")"
                        }
                    ],
                    "expected": "No exception thrown; Message.isSigned() returns false; Message.isVerified() returns false; internal tsigState equals Message.TSIG_UNSIGNED (0); Message.getTSIG() returns null",
                    "description": "Calling setTSIG with a properly constructed non‑null TSIG key should succeed without throwing and leave the message in an unsigned state. The observable behavior is that the message reports not signed and not verified, and no TSIGRecord is present yet."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"_test._tsig.example.com.\"), \"hmac-md5\", \"YWJjZDEyMzQ1\")"
                        }
                    ],
                    "expected": "No exception thrown; after setTSIG, Message.getHeader().getAdditionalCount() remains unchanged (no TSIG added yet)",
                    "description": "A TSIG key whose name is an explicit TSIG name should be accepted. The method should not automatically add a TSIGRecord to the additional section; the count stays the same."
                }
            ]
        },
        {
            "group": "Replace existing TSIG key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "firstKey = new TSIG(Name.fromString(\"first.example.com.\"), \"hmac-sha1\", \"c2VjcmV0MQ==\")"
                        },
                        {
                            "parameter": "key",
                            "value": "secondKey = new TSIG(Name.fromString(\"second.example.com.\"), \"hmac-sha256\", \"c2VjcmV0Mg==\")"
                        }
                    ],
                    "expected": "First call to setTSIG(firstKey) succeeds; second call to setTSIG(secondKey) succeeds; internal tsigState is TSIG_UNSIGNED after each call; the key stored after the second call equals secondKey; no exception is thrown",
                    "description": "When setTSIG is invoked twice with different keys, the second invocation should replace the previous key and reset the TSIG state. This verifies that the method does not retain the old key."
                }
            ]
        },
        {
            "group": "TSIG state after setting",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"state.example.com.\"), \"hmac-sha256\", \"c2VjcmV0\")"
                        }
                    ],
                    "expected": "After setTSIG, reflective access to private field tsigState yields value Message.TSIG_UNSIGNED (0); tsigstart equals -1; isSigned() returns false; isVerified() returns false",
                    "description": "The overloaded setTSIG(TSIG) delegates to setTSIG(key, Rcode.NOERROR, null) which should initialise the internal TSIG bookkeeping fields. The test confirms the exact values of tsigState and tsigstart."
                }
            ]
        },
        {
            "group": "Idempotent call with same TSIG key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "sharedKey = new TSIG(Name.fromString(\"same.example.com.\"), \"hmac-md5\", \"c2VjcmV0\")"
                        }
                    ],
                    "expected": "Calling setTSIG(sharedKey) twice does not change the internal state after the first call; tsigState remains TSIG_UNSIGNED and tsigstart stays -1; no exception is thrown",
                    "description": "Ensures that invoking setTSIG repeatedly with the same key is safe and does not corrupt internal bookkeeping."
                }
            ]
        },
        {
            "group": "TSIG key with empty secret",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(Name.fromString(\"emptysecret.example.com.\"), \"hmac-sha256\", \"\")"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown (TSIG constructor validates secret length)",
                    "description": "Even though setTSIG itself only checks for null, constructing a TSIG with an empty secret is illegal; this case verifies that such a key cannot be passed to setTSIG."
                }
            ]
        }
    ]
}