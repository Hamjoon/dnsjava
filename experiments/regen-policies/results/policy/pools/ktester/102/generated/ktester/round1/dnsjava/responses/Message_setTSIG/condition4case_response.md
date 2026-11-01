{
    "testCases": [
        {
            "group": "Valid TSIG addition (default NOERROR)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "Message.isSigned() == true && Message.getTSIG() != null && Message.getTSIG().getAlgorithm().equals(\"hmac-md5\")",
                    "description": "Calling setTSIG(TSIG) with a non‑null TSIG key should attach a TSIGRecord to the message, set the internal state to TSIG_SIGNED and make isSigned() return true."
                }
            ]
        },
        {
            "group": "Null TSIG removal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.isSigned() == false && Message.getTSIG() == null && Message.tsigState == Message.TSIG_UNSIGNED",
                    "description": "Passing null to setTSIG(TSIG) should clear any existing TSIG, reset the state to TSIG_UNSIGNED and ensure the message is not considered signed."
                }
            ]
        },
        {
            "group": "Error code handling (non‑NOERROR)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha256\", \"key\".getBytes())"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.FORMERR"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_FAILED && Message.isSigned() == false",
                    "description": "When the three‑argument overload is invoked with an error code other than NOERROR, the method should mark the TSIG processing as failed (TSIG_FAILED) and the message must not be considered signed."
                }
            ]
        },
        {
            "group": "Existing TSIG replacement",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"old\".getBytes())"
                        }
                    ],
                    "expected": "Message.getTSIG().getKey().equals(\"old\")",
                    "description": "First call attaches an initial TSIG."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha1\", \"new\".getBytes())"
                        }
                    ],
                    "expected": "Message.getTSIG().getAlgorithm().equals(\"hmac-sha1\") && Message.isSigned() == true",
                    "description": "Calling setTSIG again with a different key should replace the existing TSIGRecord and keep the state as TSIG_SIGNED."
                }
            ]
        },
        {
            "group": "Verification with query TSIG (successful verification)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, key, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_VERIFIED && Message.isVerified() == true",
                    "description": "When a query TSIGRecord is supplied and the MAC verification succeeds, the method should set the state to TSIG_VERIFIED and isVerified() must return true."
                }
            ]
        },
        {
            "group": "Verification with query TSIG (failed verification)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, new TSIG(\"hmac-md5\", \"wrong\".getBytes()), new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_FAILED && Message.isVerified() == false",
                    "description": "If the supplied query TSIGRecord does not match the key (MAC verification fails), the method must transition to TSIG_FAILED and verification flag stays false."
                }
            ]
        },
        {
            "group": "Intermediate state handling (TSIG_INTERMEDIATE)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_INTERMEDIATE && Message.isSigned() == false",
                    "description": "If the method is called during message construction (no query TSIG and no error), it should place the TSIG in an intermediate state awaiting final signing."
                }
            ]
        }
    ]
}