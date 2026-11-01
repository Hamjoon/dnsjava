{
    "test_cases": [
        {
            "group": "Invalid Key Length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with TSIG algorithm HMAC_SHA256"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[0]"
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
                    "description": "A zero‑length secret key is not valid for HMAC‑SHA256; the method must detect the insufficient key size and throw IAE."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with TSIG algorithm HMAC_MD5"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x01}"
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
                    "description": "A key that is shorter than the minimum required length for the selected algorithm (MD5 expects at least 8 bytes) must cause IAE."
                }
            ]
        },
        {
            "group": "Invalid Error Code",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with default TSIG settings"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x0A,0x0B}"
                        },
                        {
                            "parameter": "int error",
                            "value": "9999"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Error codes outside the DNS RCODE range (0‑65535) are invalid; the implementation should validate and throw IAE for an out‑of‑range value."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with default TSIG settings"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x0A,0x0B}"
                        },
                        {
                            "parameter": "int error",
                            "value": "-5"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Negative error codes are not permitted; the method must reject them with IAE."
                }
            ]
        },
        {
            "group": "Old Record Missing Required Fields",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with TSIG algorithm HMAC_SHA1"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x01,0x02,0x03,0x04}"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "mock(TSIGRecord) with time = null, name = HMAC_SHA1, MAC = non‑null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "An old TSIGRecord whose timestamp is null cannot be used for chaining; the method should throw IAE."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with TSIG algorithm HMAC_SHA256"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x0A,0x0B,0x0C,0x0D}"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "mock(TSIGRecord) with name = null, time = Instant.now(), MAC = non‑null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "An old TSIGRecord without an algorithm name cannot be processed; the method must reject it with IAE."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "mock(Message) with TSIG algorithm HMAC_SHA384"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "new byte[]{0x01,0x02,0x03,0x04,0x05,0x06}"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "mock(TSIGRecord) with name = HMAC_SHA384, time = Instant.now(), MAC = null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the previous TSIGRecord has a null MAC, the chaining logic cannot be applied; the implementation should treat this as an invalid old record and throw IAE."
                }
            ]
        }
    ]
}