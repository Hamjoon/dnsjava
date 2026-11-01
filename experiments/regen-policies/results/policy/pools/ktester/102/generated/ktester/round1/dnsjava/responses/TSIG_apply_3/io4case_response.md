Invalid control character at: line 60 column 42 (char 2852)
{
    "test_cases": [
        {
            "group": "apply_NormalSignature_WithNoPreviousTSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a simple query (e.g., A record for example.com) without any TSIG RR"
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
                    "expected": "Message m now contains a TSIG RR whose name is the TSIG key name, algorithm is HMAC_SHA256, error field is 0, fudge equals TSIG.FUDGE (300s), and a non‑null MAC of the correct length (32 bytes for HMAC‑SHA256). No exception is thrown.",
                    "description": "Standard signing of a fresh DNS message with no prior TSIG. The method should generate a full TSIG record and attach it to the message."
                }
            ]
        },
        {
            "group": "apply_NormalSignature_WithPreviousTSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS response that already has a TSIG RR (the \"old\" record)"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord representing the TSIG from the request (same key, same algorithm, proper timeSigned, etc.)"
                        }
                    ],
                    "expected": "Message m now contains a new TSIG RR whose MAC is calculated using the previous TSIG (old) as the prior MAC per RFC 2845. All other fields (key name, algorithm, fudge, error) are correctly populated. No exception is thrown.",
                    "description": "Signing a response that continues a TSIG‑protected transaction. The old TSIG is used as the seed for the MAC chain."
                }
            ]
        },
        {
            "group": "apply_ErrorCodeNonZero",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS query without a TSIG RR"
                        },
                        {
                            "parameter": "error",
                            "value": "9   // NOTAUTH (example non‑zero error code)"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "Message m contains a TSIG RR whose error field equals 9 and whose MAC field is empty (zero‑length byte array). All other fields (key name, algorithm, timeSigned, fudge) are present. No exception is thrown.",
                    "description": "When a non‑zero error code is supplied, TSIG.apply must create a TSIG record that signals the error and must not compute a MAC."
                }
            ]
        },
        {
            "group": "apply_NullMessageThrowsNPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
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
                    "expected": "NullPointerException is thrown",
                    "description": "The method must validate that the Message argument is not null and throw NPE if it is."
                }
            ]
        },
        {
            "group": "apply_FullSignatureFlagFalse_ProducesTruncatedMAC",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS query without a TSIG RR"
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
                    "expected": "Message m contains a TSIG RR whose MAC length is 10 bytes (the default truncated length used when fullSignature=false) instead of the full algorithm length (e.g., 32 bytes for HMAC‑SHA256). All other fields are correctly populated. No exception is thrown.",
                    "description": "When the overloaded apply(Message,int,TSIGRecord,boolean) is invoked with fullSignature set to false (the public apply(Message,int,TSIGRecord) delegates to it with true, so we simulate the false case via reflection or a wrapper). The resulting TSIG should contain a truncated MAC as required for DNS‑SEC‑aware transports."
                }
            ]
        },
        {
            "group": "apply_OldTSIGWithDifferentKeyName",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS response without a TSIG RR"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with a key name that does NOT match the key name of the TSIG instance used for signing"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (or a DNSException) because the old TSIG does not belong to the same key; the method must reject mismatched prior TSIG records.",
                    "description": "Ensures that the implementation validates that the old TSIG, when present, uses the same key name as the TSIG object performing the signing."
                }
            ]
        },
        {
            "group": "apply_ClockSkewBeyondFudge",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS query where the TSIGRecord old has a timeSigned that is more than TSIG.FUDGE seconds in the past relative to Clock.systemUTC()"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with timeSigned = Instant.now().minus(Duration.ofSeconds(TSIG.FUDGE.getSeconds() + 10))"
                        }
                    ],
                    "expected": "TSIG.apply should still produce a TSIG RR (the method does not verify time on signing), but the resulting TSIG’s timeSigned field should be set to the current instant (within a few seconds). No exception is thrown.",
                    "description": "Verifies that signing does not reject a request whose prior TSIG is outside the allowed fudge window; the fudge check is performed only during verification, not during apply."
                }
            ]
        },
        {
            "group": "apply_AlgorithmMappingViaTSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS query without a TSIG RR"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "null (but the TSIG instance was constructed with algorithm name \"hmac-sha384.\")"
                        }
                    ],
                    "expected": "The generated TSIG RR uses the algorithm name HMAC_SHA384 (Name object) and the MAC length is 48 bytes. No exception is thrown.",
                    "description": "Ensures that the algorithm string supplied to the TSIG constructor is correctly mapped to the corresponding Name constant via TSIG.algorithmToName and that the MAC is computed with the right algorithm."
                }
            ]
        }
    ]
}