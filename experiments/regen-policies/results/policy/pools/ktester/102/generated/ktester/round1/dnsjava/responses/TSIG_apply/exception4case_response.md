{
    "test_cases": [
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Unsupported JCE Algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with a simple query and no TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord with algorithm name HMAC_SHA384 (Name HMAC_SHA384) but the runtime JCE provider does not support HmacSHA384"
                        }
                    ],
                    "expected": "NoSuchAlgorithmException (subclass of GeneralSecurityException)",
                    "description": "When the old TSIGRecord specifies a supported DNS algorithm name that maps to a JCE algorithm unavailable in the current environment, the apply method should attempt to instantiate the MAC and fail with NoSuchAlgorithmException."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Null Key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message containing a question section, no TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord whose key (secret) is null, algorithm HMAC_SHA256"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method creates a SecretKeySpec from the key bytes; a null key should cause a NullPointerException before any MAC computation."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Empty Key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with a single question, no TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord with an empty byte[] key, algorithm HMAC_SHA256"
                        }
                    ],
                    "expected": "InvalidKeyException (subclass of GeneralSecurityException)",
                    "description": "An empty secret key is not valid for HMAC; SecretKeySpec creation succeeds but Mac.init throws InvalidKeyException."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Past Timestamp beyond fudge",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Empty Message (no sections) without TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord whose timeSigned is current system time minus 10 minutes (exceeds FUDGE of 300 seconds in the past)"
                        }
                    ],
                    "expected": "SecurityException (or subclass of GeneralSecurityException) indicating timestamp outside allowed window",
                    "description": "If the old TSIGRecord's timeSigned is older than now - FUDGE, the method must reject it to prevent replay attacks."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Message containing duplicate TSIG records",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message that already has two TSIG RRs in the additional section"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "The TSIG specification allows at most one TSIG RR per message; encountering more than one should cause the method to throw IllegalStateException."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Null Algorithm Name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with a query, no TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord whose algorithm Name field is null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the old record's algorithm name is null, the internal lookup (algorithmToName) will attempt to operate on a null value and throw NullPointerException."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - Old Record with Invalid MAC Length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with a single question, no TSIG"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord using HMAC_SHA256 but with a MAC byte array of length 5 (should be 32 bytes)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The method validates that the MAC length matches the output size of the selected algorithm; a mismatch should cause IllegalArgumentException."
                }
            ]
        }
    ]
}