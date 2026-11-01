{
    "testCases": [
        {
            "group": "setTSIG null handling",
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
                    "expected": "Message.tsigkey == null && Message.tsigerror == 0 && Message.querytsig == null",
                    "description": "Verify that setTSIG correctly accepts null for both TSIG key and query TSIG record, leaving the fields null while storing the error code."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "null"
                        },
                        {
                            "parameter": "error",
                            "value": "5"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "TSIGRecord instance Z"
                        }
                    ],
                    "expected": "Message.tsigkey == null && Message.tsigerror == 5 && Message.querytsig == TSIGRecord instance Z",
                    "description": "Ensure that a null TSIG key does not prevent storing a non‑null query TSIG record and the supplied error code."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "TSIG instance C"
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
                    "expected": "Message.tsigkey == TSIG instance C && Message.tsigerror == 0 && Message.querytsig == null",
                    "description": "Confirm that a non‑null TSIG key can be stored while the query TSIG record is null."
                }
            ]
        },
        {
            "group": "setTSIG error boundary values",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "TSIG instance D"
                        },
                        {
                            "parameter": "error",
                            "value": "-1"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "TSIGRecord instance W"
                        }
                    ],
                    "expected": "Message.tsigkey == TSIG instance D && Message.tsigerror == -1 && Message.querytsig == TSIGRecord instance W",
                    "description": "Test handling of a negative error code (outside typical TSIG error range)."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "TSIG instance E"
                        },
                        {
                            "parameter": "error",
                            "value": "2147483647"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "TSIGRecord instance V"
                        }
                    ],
                    "expected": "Message.tsigkey == TSIG instance E && Message.tsigerror == 2147483647 && Message.querytsig == TSIGRecord instance V",
                    "description": "Validate that the maximum int value can be stored as the error code without overflow."
                }
            ]
        },
        {
            "group": "setTSIG repeated invocations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "TSIG instance F"
                        },
                        {
                            "parameter": "error",
                            "value": "0"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "TSIGRecord instance U"
                        }
                    ],
                    "expected": "Message.tsigkey == TSIG instance F && Message.tsigerror == 0 && Message.querytsig == TSIGRecord instance U",
                    "description": "First call: store initial TSIG data."
                },
                {
                    "input": [
                        {
                            "parameter": "key",
                            "value": "TSIG instance G"
                        },
                        {
                            "parameter": "error",
                            "value": "3"
                        },
                        {
                            "parameter": "querytsig",
                            "value": "TSIGRecord instance T"
                        }
                    ],
                    "expected": "Message.tsigkey == TSIG instance G && Message.tsigerror == 3 && Message.querytsig == TSIGRecord instance T",
                    "description": "Second call: ensure that setTSIG overwrites previous values with new ones."
                }
            ]
        }
    ]
}