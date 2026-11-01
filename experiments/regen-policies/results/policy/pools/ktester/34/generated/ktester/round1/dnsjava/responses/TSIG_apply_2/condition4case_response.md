{
    "testCases": [
        {
            "group": "Null arguments handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "null"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because Message is null",
                    "description": "Verify that the method validates the Message argument and throws an exception when it is null, regardless of the other parameters."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "No exception; a new TSIGRecord is generated and added to the Message with a full signature",
                    "description": "When the old TSIGRecord is null the method should generate a fresh TSIGRecord. The fullSignature flag should cause the MAC to cover the entire message."
                }
            ]
        },
        {
            "group": "Full signature flag true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message containing a single question section"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "valid TSIGRecord with algorithm HMAC_SHA256"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "Message contains a TSIGRecord whose MAC is computed over the whole wire format of the message (including the additional section)",
                    "description": "When fullSignature is true the MAC must be calculated over the complete message. The test extracts the TSIGRecord from the Message and recomputes the MAC using the same key to verify it matches."
                }
            ]
        },
        {
            "group": "Full signature flag false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with question and answer sections"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "valid TSIGRecord with algorithm HMAC_SHA1"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "Message contains a TSIGRecord whose MAC is computed only over the DNS header and the question section (the default TSIG behaviour)",
                    "description": "When fullSignature is false the MAC should be calculated over the minimal part of the message as defined by RFC 2845. The test validates that the MAC does not include the answer section."
                }
            ]
        },
        {
            "group": "Old TSIGRecord present (re‑signing)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with an existing TSIGRecord (old) attached"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "the same TSIGRecord that is already attached to the Message"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "The old TSIGRecord is replaced by a new TSIGRecord with an updated MAC and timestamp, preserving the original key name and algorithm",
                    "description": "When an existing TSIGRecord is supplied the method must re‑sign the message, updating the MAC and the time‑signed field while keeping other fields unchanged."
                }
            ]
        },
        {
            "group": "Algorithm name mapping edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message with no additional records"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord built with algorithm name \"unknown-alg\" (unsupported)"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "UnsupportedOperationException (or IllegalArgumentException) thrown because the algorithm cannot be mapped to a known HMAC implementation",
                    "description": "The apply method should validate the algorithm name. Supplying an unknown algorithm must result in a clear exception rather than silent failure."
                }
            ]
        },
        {
            "group": "Fudge time handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message whose timestamp is set to Instant.now().minus(TSIG.FUDGE).minusSeconds(1)"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "valid TSIGRecord"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "TSIGRecord added to the Message with a time‑signed value that is within the allowed fudge window (i.e., not older than FUDGE seconds)",
                    "description": "Even though the message timestamp is older than the current time, the apply method must adjust the time‑signed field to be within the allowed fudge period."
                }
            ]
        }
    ]
}