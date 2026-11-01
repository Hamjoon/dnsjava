{
    "testCases": [
        {
            "group": "setTSIG null querytsig",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"example.net.\", \"hmac-sha256\", \"c2VjcmV0a2V5==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.tsigkey == provided TSIG object, Message.tsigerror == 0, Message.querytsig == null",
                    "description": "Verify that a non‑null TSIG key can be stored while the query TSIG record is null; the method should simply assign the fields without throwing."
                }
            ]
        },
        {
            "group": "setTSIG both key and querytsig null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "null"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.tsigkey == null, Message.tsigerror == 0, Message.querytsig == null",
                    "description": "Ensure that passing null for both the TSIG key and the query TSIG record does not cause a NullPointerException and stores nulls correctly."
                }
            ]
        },
        {
            "group": "setTSIG overwrite existing values",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"first.example.\", \"hmac-md5\", \"firstKey==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "1"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"first.example.\"), DClass.IN, 300, new TSIG(\"first.example.\", \"hmac-md5\", \"firstKey==\"), 0, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigkey == first TSIG, Message.tsigerror == 1, Message.querytsig == first TSIGRecord",
                    "description": "Initial setTSIG call to establish a baseline state."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"second.example.\", \"hmac-sha256\", \"secondKey==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "2"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"second.example.\"), DClass.IN, 300, new TSIG(\"second.example.\", \"hmac-sha256\", \"secondKey==\"), 0, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigkey == second TSIG, Message.tsigerror == 2, Message.querytsig == second TSIGRecord (previous values replaced)",
                    "description": "Call setTSIG a second time with different arguments and verify that the fields are overwritten, not appended."
                }
            ]
        },
        {
            "group": "setTSIG error code boundaries",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"boundary.example.\", \"hmac-md5\", \"key==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "-1"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"boundary.example.\"), DClass.IN, 300, new TSIG(\"boundary.example.\", \"hmac-md5\", \"key==\"), 0, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigkey == provided TSIG, Message.tsigerror == -1, Message.querytsig == provided TSIGRecord",
                    "description": "Test that a negative error code (outside typical DNS error range) is stored verbatim; the method does not validate the error value."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"boundary.example.\", \"hmac-md5\", \"key==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "65535"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"boundary.example.\"), DClass.IN, 300, new TSIG(\"boundary.example.\", \"hmac-md5\", \"key==\"), 0, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigkey == provided TSIG, Message.tsigerror == 65535, Message.querytsig == provided TSIGRecord",
                    "description": "Test the upper 16‑bit boundary for the error code; the method should accept the value unchanged."
                }
            ]
        },
        {
            "group": "setTSIG with querytsig containing null TSIG key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "new TSIG(\"outer.example.\", \"hmac-md5\", \"outerKey==\")"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "new TSIGRecord(Name.fromString(\"outer.example.\"), DClass.IN, 300, null, 0, new byte[0])"
                        }
                    ],
                    "expected": "Message.tsigkey == outer TSIG, Message.tsigerror == 0, Message.querytsig.TSIG == null",
                    "description": "Ensure that a TSIGRecord whose internal TSIG field is null can be stored; the outer TSIG key is independent and should remain set."
                }
            ]
        }
    ]
}