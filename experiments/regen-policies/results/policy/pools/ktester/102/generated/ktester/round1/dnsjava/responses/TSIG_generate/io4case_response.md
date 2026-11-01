{
    "testCases": [
        {
            "group": "Normal generation without prior TSIG (full signature)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a single A query for example.com."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x0A, 0x0B, 0x0C, 0x0D}" 
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
                    "expected": "TSIGRecord whose algorithm name is HMAC_SHA256, error code 0, and MAC equal to the result of HMAC‑SHA256(key=0x0A0B0C0D, data=Message.toWire()) (full length 32 bytes). The timeSigned field must be within ±TSIG.FUDGE of the current Instant.",
                    "description": "Verifies that generate creates a correct TSIGRecord when no previous TSIG is supplied, using the default full‑signature path."
                }
            ]
        },
        {
            "group": "Normal generation with prior TSIG (full signature)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a DNS UPDATE request for zone example.com."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x55, 0x66, 0x77, 0x88}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with algorithm HMAC_SHA256, key byte[]{0x55,0x66,0x77,0x88}, and MAC from a previous generate call."
                        }
                    ],
                    "expected": "TSIGRecord with algorithm HMAC_SHA256, error 0, and MAC equal to HMAC‑SHA256(key=0x55667788, data=Message.toWire() || old.getMAC()) (full 32‑byte MAC). The timeSigned must be within the allowed fudge window.",
                    "description": "Ensures that when a previous TSIGRecord is supplied, its MAC is incorporated into the new MAC calculation as defined by the TSIG specification."
                }
            ]
        },
        {
            "group": "Generation with non‑zero error code",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a standard query for www.example.org."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x01, 0x23, 0x45, 0x67}"
                        },
                        {
                            "parameter": "error",
                            "value": "9"   ,
                            "description": "TSIG error code BADKEY (value 9)."
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "TSIGRecord whose error field equals 9 (BADKEY) while all other fields (algorithm, MAC, timeSigned) are generated as usual.",
                    "description": "Checks that the error parameter is correctly stored in the resulting TSIGRecord."
                }
            ]
        },
        {
            "group": "Generation with empty message payload",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message with no sections (empty wire format)."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0xAA, 0xBB}"
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
                    "expected": "TSIGRecord whose MAC equals HMAC‑SHA256(key=0xAABB, data=empty byte array). The MAC length must be 32 bytes and the algorithm name HMAC_SHA256.",
                    "description": "Validates that generate correctly handles an empty DNS message without throwing exceptions."
                }
            ]
        },
        {
            "group": "Generation with fullSignature flag set to false (truncated MAC)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a TXT query for test.example."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0xDE, 0xAD, 0xBE, 0xEF}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "TSIGRecord with algorithm HMAC_SHA256, error 0, and MAC truncated to 10 bytes (the first 10 bytes of the full 32‑byte HMAC‑SHA256). The truncated MAC length must be exactly 10 bytes.",
                    "description": "Ensures that when fullSignature is false, generate returns a TSIGRecord with a shortened MAC as required for DNS‑SEC style signatures."
                }
            ]
        },
        {
            "group": "Null Message argument handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x00}"
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
                    "expected": "NullPointerException (or IllegalArgumentException) thrown immediately, before any MAC processing.",
                    "description": "Confirms that generate validates its Message argument and fails fast when it is null."
                }
            ]
        },
        {
            "group": "Algorithm mapping verification (HMAC‑SHA1)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message containing a MX query for mail.example.com."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0x11, 0x22, 0x33, 0x44}"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with algorithm HMAC_SHA1, key byte[]{0x11,0x22,0x33,0x44}"
                        }
                    ],
                    "expected": "TSIGRecord whose algorithm name is HMAC_SHA1 and whose MAC equals HMAC‑SHA1(key=0x11223344, data=Message.toWire()).",
                    "description": "Verifies that generate correctly resolves the HMAC‑SHA1 algorithm name and produces the expected MAC."
                }
            ]
        },
        {
            "group": "Fudge window boundary test",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message with a single SOA query."
                        },
                        {
                            "parameter": "b",
                            "value": "byte[]{0xFE, 0xED, 0xFA, 0xCE}"
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
                    "expected": "TSIGRecord whose timeSigned field is exactly Instant.now().plus(TSIG.FUDGE) or Instant.now().minus(TSIG.FUDGE) (depending on the internal clock usage) but never outside the ±300‑second window.",
                    "description": "Ensures that generate respects the TSIG.FUDGE duration when stamping the TSIGRecord."
                }
            ]
        }
    ]
}