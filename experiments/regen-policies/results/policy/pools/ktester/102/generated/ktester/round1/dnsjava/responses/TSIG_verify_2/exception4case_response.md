{
    "test_cases": [
        {
            "group": "Null Message object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "null"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid non‑null byte array representing a signed DNS message"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "valid TSIGRecord (algorithm HMAC_SHA256, correct time, correct MAC)"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because the Message argument is null",
                    "description": "The verify method should detect a null Message and return FORMERR rather than throwing a NullPointerException."
                }
            ]
        },
        {
            "group": "Null MAC in TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance containing a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid raw DNS message bytes"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "TSIGRecord with algorithm HMAC_SHA256, correct time, mac = null"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because the MAC field in the TSIG record is null",
                    "description": "A null MAC cannot be compared; the method should treat this as a malformed request and return FORMERR."
                }
            ]
        },
        {
            "group": "Null secret key in TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance with a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid raw DNS message bytes"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "TSIGRecord whose secret key (key field) is null, algorithm HMAC_SHA1, correct time, correct MAC"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because the secret key is null",
                    "description": "When the secret key required to initialise the Mac instance is null, the method should catch the resulting exception and return FORMERR."
                }
            ]
        },
        {
            "group": "Zero‑length secret key (InvalidKeyException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance with a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid raw DNS message bytes"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "TSIGRecord with algorithm HMAC_SHA256, correct time, secret key = new byte[0], correct MAC"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails due to InvalidKeyException during Mac initialisation",
                    "description": "A zero‑length secret key is not a valid HMAC key; Mac.init throws InvalidKeyException which should be caught and result in FORMERR."
                }
            ]
        },
        {
            "group": "Null timeSigned in TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance with a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid raw DNS message bytes"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "TSIGRecord with algorithm HMAC_SHA256, timeSigned = null, correct MAC"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because the timestamp is missing",
                    "description": "If the TSIGRecord does not contain a timestamp, the verifier cannot perform the time‑skew check and must return FORMERR."
                }
            ]
        },
        {
            "group": "Message without TSIG RR but requestTSIG null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance that does NOT contain a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "valid raw DNS message bytes"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "null"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because both the message and requestTSIG lack TSIG information",
                    "description": "When the DNS message has no TSIG RR and the caller also supplies a null requestTSIG, the method should treat the request as malformed."
                }
            ]
        },
        {
            "group": "Message bytes length of one (insufficient data)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "Message instance with a TSIG RR"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[1] (single‑byte array)"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "valid TSIGRecord (algorithm HMAC_SHA1, correct time, correct MAC)"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "1 (Rcode.FORMERR) – verification fails because the raw message is too short to contain a valid DNS header",
                    "description": "The verifier expects at least the DNS header length; a one‑byte array should trigger a format error."
                }
            ]
        }
    ]
}