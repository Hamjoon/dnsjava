Invalid control character at: line 10 column 52 (char 319)
{
    "testCases": [
        {
            "group": "apply – no error, no previous TSIG, full signature (default)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message() // empty DNS message"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.NOERROR // 0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "Message m now contains a TSIG RR generated with the default algorithm (HMAC-MD5) and a full 16‑byte MAC. No exception is thrown. The TSIG RR’s error field is 0 (NOERROR).",
                    "description": "Verify that apply(Message,0,null) creates a new TSIG record with a full signature when there is no prior TSIG and the error code is NOERROR."
                }
            ]
        },
        {
            "group": "apply – no error, existing previous TSIG, full signature",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message() // contains a query section"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.NOERROR // 0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "new TSIGRecord(Name.fromString(\"example.com.\"), TSIG.HMAC_SHA256, 300, new byte[32]) // a prior TSIG"
                        }
                    ],
                    "expected": "Message m now contains a TSIG RR whose MAC is calculated using the previous TSIG as the “previous MAC” per RFC2845. The error field stays 0. No exception is thrown.",
                    "description": "When a previous TSIGRecord is supplied, apply should use its MAC as the starting point for the new MAC calculation (full signature)."
                }
            ]
        },
        {
            "group": "apply – error BADTIME (clock skew), no previous TSIG, full signature",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.BADTIME // 18"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "Message m contains a TSIG RR with error field set to BADTIME. The MAC is still generated (full signature) but the error code signals a time‑skew problem. No exception is thrown.",
                    "description": "The method must propagate a non‑zero error code (BADTIME) into the generated TSIG record while still producing a signature."
                }
            ]
        },
        {
            "group": "apply – error BADTIME, existing previous TSIG, full signature",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.BADTIME // 18"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "new TSIGRecord(Name.fromString(\"example.org.\"), TSIG.HMAC_SHA1, 300, new byte[20])"
                        }
                    ],
                    "expected": "Message m contains a TSIG RR with error BADTIME and a MAC calculated using the previous TSIG’s MAC as the seed. No exception is thrown.",
                    "description": "Combine a non‑zero error with a prior TSIG to ensure both branches (error handling and previous‑MAC usage) are exercised."
                }
            ]
        },
        {
            "group": "apply – no error, no previous TSIG, **partial** signature (fullSignature = false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.NOERROR // 0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "Message m contains a TSIG RR whose MAC length is truncated to 10 bytes (as defined for partial signatures). The error field is 0. No exception is thrown.",
                    "description": "The overloaded apply method with fullSignature = false must generate a truncated MAC (partial signature)."
                }
            ]
        },
        {
            "group": "apply – no error, existing previous TSIG, **partial** signature (fullSignature = false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "int error",
                            "value": "Rcode.NOERROR // 0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "new TSIGRecord(Name.fromString(\"example.net.\"), TSIG.HMAC_SHA384, 300, new byte[48])"
                        }
                    ],
                    "expected": "Message m contains a TSIG RR with a 12‑byte truncated MAC (partial signature) that was derived using the previous MAC as seed. No exception is thrown.",
                    "description": "Verify that a prior TSIG is honoured even when only a partial signature is requested."
                }
            ]
        },
        {
            "group": "algorithmToName – mapping of known algorithm strings",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "String alg",
                            "value": "\"hmac-sha256\""
                        }
                    ],
                    "expected": "TSIG.HMAC_SHA256 // Name instance representing the SHA‑256 algorithm",
                    "description": "The helper should translate a known algorithm identifier to the corresponding static Name constant."
                },
                {
                    "input": [
                        {
                            "parameter": "String alg",
                            "value": "\"HMAC-MD5.SIG-ALG.REG.INT.\""
                        }
                    ],
                    "expected": "TSIG.HMAC_MD5",
                    "description": "Case‑insensitive mapping for the MD5 algorithm."
                },
                {
                    "input": [
                        {
                            "parameter": "String alg",
                            "value": "\"unknown-alg\""
                        }
                    ],
                    "expected": "null // algorithm not recognised",
                    "description": "When the algorithm string is not known, the method should return null."
                }
            ]
        }
    ]
}