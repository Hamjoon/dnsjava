{
    "test_cases": [
        {
            "group": "Null argument handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x01,0x02}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "old",
                            "value": "mockOldRecord"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the Message argument is null the method (or the delegated private generate) should throw a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage"
                        },
                        {
                            "parameter": "b",
                            "value": "null"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "old",
                            "value": "mockOldRecord"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the byte[] argument is null the method (or the delegated private generate) should throw a NullPointerException."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x0A}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.BADTIME"
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
                    "expected": "NullPointerException",
                    "description": "When the old TSIGRecord argument is null the method (or the delegated private generate) should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "initHmac exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x01}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "old",
                            "value": "mockOldRecord"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "RuntimeException (wrapped GeneralSecurityException from initHmac)",
                    "description": "When initHmac() throws a checked security exception (e.g., NoSuchAlgorithmException) wrapped in a RuntimeException, the generate method must propagate that RuntimeException unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x02,0x03}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.BADTRUNC"
                        },
                        {
                            "parameter": "old",
                            "value": "mockOldRecord"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "RuntimeException (wrapped GeneralSecurityException from initHmac)",
                    "description": "Even for error BADTRUNC, if initHmac() fails with a RuntimeException, the exception must bubble up to the caller."
                }
            ]
        }
    ]
}