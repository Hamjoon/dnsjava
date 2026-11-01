{
    "test_cases": [
        {
            "group": "apply - null message argument",
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
                    "expected": "NullPointerException thrown (m is null)",
                    "description": "When the Message argument is null, apply() should immediately throw a NullPointerException while trying to invoke m.toWire() or m.addRecord()."
                }
            ]
        },
        {
            "group": "apply - Message.toWire throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message mock where toWire() throws IllegalStateException(\"wire error\")"
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
                    "expected": "IllegalStateException with message \"wire error\" propagates out of apply()",
                    "description": "If Message.toWire() throws a runtime exception, apply() must not catch it; the exception should be propagated to the caller."
                }
            ]
        },
        {
            "group": "apply - generate throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed"
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
                    "expected": "RuntimeException (e.g., GeneralSecurityException wrapped) thrown by apply()",
                    "description": "Using a Mockito spy on TSIG, stub the public generate(Message, byte[], int, TSIGRecord, boolean) to throw a RuntimeException. apply() should let this exception bubble up."
                }
            ]
        },
        {
            "group": "apply - negative error code triggers IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed"
                        },
                        {
                            "parameter": "error",
                            "value": "-1"
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
                    "expected": "IllegalArgumentException thrown (error code must be non‑negative)",
                    "description": "If an invalid (negative) DNS error code is supplied, generate() is expected to validate the argument and throw IllegalArgumentException, which apply() must propagate."
                }
            ]
        },
        {
            "group": "apply - old TSIGRecord with null algorithm throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "old",
                            "value": "TSIGRecord mock where getAlgorithm() returns null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown (old TSIGRecord has null algorithm)",
                    "description": "When a previous TSIGRecord is supplied but its algorithm name is null, generate() should reject the input and throw IllegalArgumentException; apply() must not swallow this exception."
                }
            ]
        }
    ]
}