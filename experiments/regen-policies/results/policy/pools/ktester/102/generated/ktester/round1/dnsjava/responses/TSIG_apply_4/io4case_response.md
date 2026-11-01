Invalid control character at: line 122 column 42 (char 5548)
{
    "testCases": [
        {
            "group": "apply_NoOldTSIG_FullSignatureFalse",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message instance with empty additional section"
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
                    "expected": "Message contains exactly one TSIGRecord in the ADDITIONAL section; the TSIGRecord is generated with error=0, old=null, fullSignature=false; m.tsigState == Message.TSIG_SIGNED",
                    "description": "Verify that apply creates a TSIGRecord when no previous TSIG is supplied and fullSignature flag is false, adds it to the message and updates the TSIG state."
                }
            ]
        },
        {
            "group": "apply_NoOldTSIG_FullSignatureTrue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message instance with a single query record in QUESTION section"
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
                            "value": "true"
                        }
                    ],
                    "expected": "Message contains a TSIGRecord in the ADDITIONAL section; the generated TSIGRecord includes the full signature (covers the whole message wire); m.tsigState == Message.TSIG_SIGNED",
                    "description": "Ensure that when fullSignature is true, apply still generates a TSIGRecord (full signature) even when there is no previous TSIG."
                }
            ]
        },
        {
            "group": "apply_WithOldTSIG_FullSignatureFalse",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message instance with an existing TSIGRecord in ADDITIONAL section"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord representing a previous transaction (different key name or time)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "A new TSIGRecord is added (second TSIG) to the ADDITIONAL section; the new record is generated using the supplied old TSIGRecord for MAC chaining; m.tsigState == Message.TSIG_SIGNED",
                    "description": "Validate that apply correctly handles an existing TSIG (old) and creates a new TSIGRecord that chains the MAC with the old record when fullSignature is false."
                }
            ]
        },
        {
            "group": "apply_WithOldTSIG_FullSignatureTrue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message instance with an existing TSIGRecord in ADDITIONAL section"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord with matching key name and time as the new TSIG"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "A new TSIGRecord is added; because fullSignature is true the MAC is calculated over the full wire including the old TSIG; m.tsigState == Message.TSIG_SIGNED",
                    "description": "Check that when fullSignature is true the generate method uses the full message (including old TSIG) for the MAC, and apply still adds the new TSIG."
                }
            ]
        },
        {
            "group": "apply_WithNonZeroErrorCode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message instance with a standard query"
                        },
                        {
                            "parameter": "error",
                            "value": "9   // NOTIMP (example DNS error code)"
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
                    "expected": "Generated TSIGRecord contains the supplied error code (9) in its RCODE field; Message tsigState is TSIG_SIGNED",
                    "description": "Confirm that the error parameter is propagated into the generated TSIGRecord."
                }
            ]
        },
        {
            "group": "apply_NullMessageThrows",
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
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "The method should not accept a null Message; verify that a NullPointerException (or appropriate IllegalArgumentException) is thrown."
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
                            "value": "Message with a TSIGRecord whose timeSigned is more than FUDGE (300s) older than the current Clock"
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
                    "expected": "Generated TSIGRecord contains error code BADSIG (or appropriate DNS error) because the time is outside the allowed fudge window; m.tsigState == Message.TSIG_SIGNED",
                    "description": "Even though apply does not directly verify time, it forwards the error code supplied; this case ensures that callers can pass a BADSIG error when the time is out of sync."
                }
            ]
        }
    ]
}