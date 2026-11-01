{
    "groups": [
        {
            "group": "Idempotent setTSIG on already signed message",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha256\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "Message.isSigned() == true && Message.getTSIG() != null && Message.tsigState == Message.TSIG_SIGNED && Message.getTSIG().getAlgorithm().equals(\"hmac-sha256\")",
                    "description": "First call attaches a TSIG and marks the message as signed. A second call with the same TSIG instance should leave the message in the same TSIG_SIGNED state without creating a second TSIGRecord."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha256\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "Message.isSigned() == true && Message.getTSIG() != null && Message.tsigState == Message.TSIG_SIGNED && Message.getTSIG().getAlgorithm().equals(\"hmac-sha256\")",
                    "description": "Calling setTSIG(TSIG) twice consecutively with an identical key must be a no‑op after the first successful attachment; the internal state and TSIGRecord remain unchanged."
                }
            ]
        },
        {
            "group": "Set TSIG with zero‑length secret",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", new byte[0])"
                        }
                    ],
                    "expected": "Message.isSigned() == true && Message.getTSIG() != null && Message.getTSIG().getKey().length == 0 && Message.tsigState == Message.TSIG_SIGNED",
                    "description": "A TSIG whose secret is an empty byte array is still a valid key for the library; setTSIG should attach it and mark the message as signed."
                }
            ]
        },
        {
            "group": "Set TSIG with unsupported algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"unsupported-algo\", \"key\".getBytes())"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the algorithm string is not one of the algorithms supported by the TSIG implementation, setTSIG(TSIG) must throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Set TSIG after successful verification resets verification flag",
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
                            "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, new TSIG(\"hmac-md5\", \"secret\".getBytes()), new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_VERIFIED && Message.isVerified() == true",
                    "description": "First, use the three‑argument overload to simulate a verified incoming TSIG (state = TSIG_VERIFIED)."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "Message.isSigned() == true && Message.isVerified() == false && Message.tsigState == Message.TSIG_SIGNED",
                    "description": "Calling the one‑argument setTSIG(TSIG) after a verified state must replace the TSIGRecord and reset the verification flag, leaving the message in the normal signed state."
                }
            ]
        },
        {
            "group": "Replace TSIG with same algorithm but different secret",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha1\", \"first\".getBytes())"
                        }
                    ],
                    "expected": "Message.getTSIG().getKey().length == \"first\".getBytes().length && Message.isSigned() == true",
                    "description": "Attach an initial TSIG using algorithm hmac‑sha1 and secret \"first\"."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-sha1\", \"second\".getBytes())"
                        }
                    ],
                    "expected": "Message.getTSIG().getKey().length == \"second\".getBytes().length && Message.getTSIG().getAlgorithm().equals(\"hmac-sha1\") && Message.isSigned() == true && Message.tsigState == Message.TSIG_SIGNED",
                    "description": "Calling setTSIG again with the same algorithm but a different secret must replace the existing TSIGRecord while keeping the message signed."
                }
            ]
        },
        {
            "group": "Set TSIG after intermediate state transition",
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
                    "description": "Use the three‑argument overload to place the message in the intermediate state (no query TSIG, no error)."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
                        }
                    ],
                    "expected": "Message.tsigState == Message.TSIG_SIGNED && Message.isSigned() == true",
                    "description": "Calling the one‑argument setTSIG(TSIG) after an intermediate state should finalize the TSIG, moving the state to TSIG_SIGNED and marking the message as signed."
                }
            ]
        }
    ]
}