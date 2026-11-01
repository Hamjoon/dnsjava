{
    "testCases": [
        {
            "group": "Delegation to overload with NOERROR",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.com.\"), TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "apply(Message, int, TSIGRecord, boolean) is invoked with error = Rcode.NOERROR and fullSignature = true",
                    "description": "When apply(Message, TSIGRecord, true) is called, it should delegate to the four‑argument overload passing Rcode.NOERROR and the same fullSignature flag."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "new Message()"
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
                    "expected": "apply(Message, int, TSIGRecord, boolean) is invoked with error = Rcode.NOERROR and fullSignature = false",
                    "description": "When old TSIGRecord is null and fullSignature is false, the method must still delegate correctly with error = NOERROR."
                }
            ]
        },
        {
            "group": "Null handling for Message argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.com.\"), TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method should throw a NullPointerException when the Message argument is null, because the underlying overload requires a non‑null Message."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
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
                    "expected": "NullPointerException",
                    "description": "Even when both Message and old TSIGRecord are null, the method must fail fast with NPE for the Message parameter."
                }
            ]
        },
        {
            "group": "Effect of fullSignature flag on generated TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A)"
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
                    "expected": "Resulting Message contains a TSIG RR with the full MAC (i.e., MAC length equals algorithm’s output size)",
                    "description": "When fullSignature is true the apply method should generate a TSIG record that includes the complete MAC."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A)"
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
                    "expected": "Resulting Message contains a TSIG RR with a truncated MAC (e.g., first 10 bytes) according to the algorithm’s default truncation policy",
                    "description": "When fullSignature is false the apply method should generate a TSIG record with a truncated MAC as defined by the algorithm."
                }
            ]
        },
        {
            "group": "Interaction with existing TSIGRecord (old) parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A)"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.com.\"), TSIG.HMAC_SHA256, 0, new byte[]{0x01,0x02,0x03,0x04}, 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "apply uses the supplied old TSIGRecord as the previous signature context (e.g., for time‑skew checking) and still produces a new TSIG RR attached to the Message",
                    "description": "When an old TSIGRecord is provided, the method must incorporate it into the signing process while still adding a fresh TSIG RR to the message."
                }
            ]
        }
    ]
}