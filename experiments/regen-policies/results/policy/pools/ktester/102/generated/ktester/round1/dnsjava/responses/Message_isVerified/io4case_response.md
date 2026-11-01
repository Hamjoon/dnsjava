{
    "tests": [
        {
            "group": "Message.isVerified() - false branch (non‑verified states)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 0
                        }
                    ],
                    "expected": false,
                    "description": "When tsigState is TSIG_UNSIGNED (0) the message is not verified."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 2
                        }
                    ],
                    "expected": false,
                    "description": "When tsigState is TSIG_INTERMEDIATE (2) the message is not verified."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 3
                        }
                    ],
                    "expected": false,
                    "description": "When tsigState is TSIG_SIGNED (3) the message is not verified."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 4
                        }
                    ],
                    "expected": false,
                    "description": "When tsigState is TSIG_FAILED (4) the message is not verified."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": -1
                        }
                    ],
                    "expected": false,
                    "description": "Negative tsigState values are not recognised as verified."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 5
                        }
                    ],
                    "expected": false,
                    "description": "Values beyond the defined constants should also return false."
                }
            ]
        },
        {
            "group": "Message.isVerified() - default construction",
            "cases": [
                {
                    "input": [],
                    "expected": false,
                    "description": "A newly created Message has the default tsigState (0) and therefore is not verified."
                }
            ]
        },
        {
            "group": "Message.isVerified() - idempotent check",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 1
                        }
                    ],
                    "expected": true,
                    "description": "Calling isVerified() repeatedly on a verified message should consistently return true."
                },
                {
                    "input": [
                        {
                            "parameter": "tsigState",
                            "value": 0
                        }
                    ],
                    "expected": false,
                    "description": "Calling isVerified() repeatedly on an unverified message should consistently return false."
                }
            ]
        }
    ]
}