{
    "test_cases": [
        {
            "group": "Null secret key handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The TSIG instance was constructed with a null secret key. Calling apply should detect the missing secret and throw NPE before any MAC computation."
                }
            ]
        },
        {
            "group": "Unsupported algorithm name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The TSIG object was created with an algorithm name that is not recognised (e.g., \"HMAC-FOO\"). apply should fail during MAC initialisation and raise an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Future timeSigned beyond fudge window",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord with timeSigned = Instant.now().plus(TSIG.FUDGE).plusSeconds(1)"
                        }
                    ],
                    "expected": "TSIGException (BADTIME)",
                    "description": "When the old TSIG’s timeSigned is in the future beyond the allowed fudge period, apply must reject the request and surface a BADTIME‑related TSIGException."
                }
            ]
        },
        {
            "group": "Old TSIG MAC length mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord using HMAC_SHA256 but with a truncated MAC (e.g., 10 bytes instead of 32)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The old TSIG record contains a MAC whose length does not match the output size of the configured algorithm. apply should validate the MAC length and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Error code out of valid range",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "70000"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "DNS RCODE values are limited to 0‑65535. Supplying a value larger than this should cause apply to reject the argument with IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Multiple TSIG records already present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) containing two TSIG records"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "A DNS message may contain at most one TSIG. If the message already holds two TSIG records, apply should detect the inconsistency and throw IllegalStateException."
                }
            ]
        },
        {
            "group": "Old TSIG key name mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with no TSIG"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord whose key name differs from the TSIG instance’s key name"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the supplied old TSIG uses a different key name than the TSIG object’s configured name, the method must reject the call with IllegalArgumentException."
                }
            ]
        }
    ]
}