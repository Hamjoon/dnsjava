{
    "test_cases": [
        {
            "group": "Old TSIGRecord null fields",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "validKeyBytes"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "oldRecordWithNullSignature"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the supplied old TSIGRecord has a null signature field, generate attempts to copy the previous MAC and throws NPE."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "validKeyBytes"
                        },
                        {
                            "parameter": "int error",
                            "value": "0"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "oldRecordWithNullName"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the old TSIGRecord's name (key name) is null, generate attempts to reference it while building the new TSIGRecord and throws NPE."
                }
            ]
        },
        {
            "group": "Message missing algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "messageWithoutTSIGAlgorithm"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "validKeyBytes"
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
                    "description": "When the Message does not contain a TSIG algorithm name (null), generate cannot map the algorithm and throws IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "messageWithEmptyAlgorithmString"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "validKeyBytes"
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
                    "expected": "GeneralSecurityException",
                    "description": "An empty algorithm string cannot be mapped to a known HMAC algorithm; generate attempts to obtain a Mac instance and throws GeneralSecurityException."
                }
            ]
        },
        {
            "group": "Invalid key for MAC initialization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessageSHA256"
                        },
                        {
                            "parameter": "byte[] b",
                            "value": "invalidKeyForSHA256"
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
                    "expected": "InvalidKeyException",
                    "description": "Supplying a key that the underlying JCE provider deems invalid for the selected HMAC algorithm (e.g., a key with illegal length for a hardware‑bound provider) causes Mac.init() to throw InvalidKeyException, which propagates as a GeneralSecurityException."
                }
            ]
        }
    ]
}