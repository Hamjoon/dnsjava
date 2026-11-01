{
    "group": "Message.isSigned_edge",
    "cases": [
        {
            "input": [
                {
                    "parameter": "tsigState",
                    "value": -1
                }
            ],
            "expected": false,
            "description": "tsigState is a negative value (-1); isSigned() should return false, confirming that out‑of‑range negative states are treated as unsigned."
        },
        {
            "input": [
                {
                    "parameter": "tsigState",
                    "value": 5
                }
            ],
            "expected": false,
            "description": "tsigState is just above the highest defined constant (5); isSigned() should return false, ensuring values beyond TSIG_FAILED are not mistakenly considered signed."
        },
        {
            "input": [
                {
                    "parameter": "tsigState",
                    "value": 2147483647
                }
            ],
            "expected": false,
            "description": "tsigState is Integer.MAX_VALUE; isSigned() should return false, verifying behavior with extreme positive integers."
        },
        {
            "input": [
                {
                    "parameter": "tsigState",
                    "value": -2147483648
                }
            ],
            "expected": false,
            "description": "tsigState is Integer.MIN_VALUE; isSigned() should return false, confirming that extreme negative integers are also treated as unsigned."
        }
    ]
}