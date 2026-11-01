{
    "groups": [
        {
            "group": "Null Field Inside TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithNullAlgorithm()"
                        }
                    ],
                    "expected": "Returns TSIG.BADKEY (value = 16)",
                    "description": "TSIGRecord has a null algorithm name – the method cannot map the algorithm to a MAC instance and should return BADKEY."
                },
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithNullKeyName()"
                        }
                    ],
                    "expected": "Returns TSIG.BADKEY (value = 16)",
                    "description": "TSIGRecord contains a null key name – MAC initialization fails and the method should return BADKEY."
                },
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithNullSignature()"
                        }
                    ],
                    "expected": "Returns TSIG.BADSIG (value = 16)",
                    "description": "Signature field inside TSIGRecord is null – verification cannot succeed and should return BADSIG."
                },
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithEmptySignature()"
                        }
                    ],
                    "expected": "Returns TSIG.BADSIG (value = 16)",
                    "description": "Signature array is empty – MAC comparison fails, resulting in BADSIG."
                }
            ]
        },
        {
            "group": "Invalid Key Data",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithZeroLengthKey()"
                        }
                    ],
                    "expected": "Returns TSIG.BADKEY (value = 16)",
                    "description": "Key data length is zero – MAC initialization throws InvalidKeyException and the method should return BADKEY."
                }
            ]
        },
        {
            "group": "Empty Message Bytes",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessage()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "new byte[0]"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithValidSignature()"
                        }
                    ],
                    "expected": "Returns TSIG.BADSIG (value = 16)",
                    "description": "The wire‑format byte array is empty – MAC is computed over an empty payload, which will not match a non‑empty signature, causing BADSIG."
                }
            ]
        },
        {
            "group": "Message Serialization Failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message",
                            "value": "mockMessageThrowsIOException()"
                        },
                        {
                            "parameter": "byte[] messageBytes",
                            "value": "validMessageBytes()"
                        },
                        {
                            "parameter": "TSIGRecord requestTSIG",
                            "value": "tsigRecordWithValidSignature()"
                        }
                    ],
                    "expected": "Returns TSIG.BADSIG (value = 16)",
                    "description": "Message.toWire() throws an IOException – the method catches the exception and treats the verification as failed, returning BADSIG."
                }
            ]
        }
    ]
}