{
    "testCases": [
        {
            "group": "testVerify_NullMessage",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "null"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00,0x01,0x02}"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "validTSIGRecord()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling verify with a null Message should throw NullPointerException because the method dereferences the Message object."
                }
            ]
        },
        {
            "group": "testVerify_NullMessageBytes",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "null"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "validTSIGRecord()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null byte array for the raw DNS message should cause a NullPointerException when the method attempts to read the array."
                }
            ]
        },
        {
            "group": "testVerify_NullTSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00,0x01,0x02}"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A null TSIGRecord argument should raise NullPointerException because the method accesses fields of the record."
                }
            ]
        },
        {
            "group": "testVerify_UnsupportedAlgorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00,0x01,0x02}"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithAlgorithm(\"UNKNOWN-ALG\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the TSIGRecord contains an algorithm name that cannot be mapped to a known HMAC algorithm, the internal Mac.getInstance call fails and the method throws IllegalArgumentException."
                }
            ]
        },
        {
            "group": "testVerify_NullKeyNameInTSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00,0x01,0x02}"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithNullKeyName()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A TSIGRecord whose key name is null should cause a NullPointerException when the method attempts to look up the secret key."
                }
            ]
        },
        {
            "group": "testVerify_EmptySignatureInTSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00,0x01,0x02}"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithEmptySignature()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the TSIGRecord contains an empty (zero‑length) signature, verification cannot be performed and the method throws IllegalArgumentException."
                }
            ]
        },
        {
            "group": "testVerify_InvalidMessageBytesLength",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "validMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[]{0x00}"  ,
                            "note": "Too short to contain a DNS header (needs at least 12 bytes)"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "validTSIGRecord()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the raw message byte array is shorter than the minimum DNS header size, the method should detect the malformed input and throw IllegalArgumentException."
                }
            ]
        }
    ]
}