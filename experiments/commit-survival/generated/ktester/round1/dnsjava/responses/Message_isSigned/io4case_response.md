{
    "group": "Message.isSigned() - additional edge cases",
    "cases": [
        {
            "input": [],
            "expected": false,
            "description": "tsigState is a negative value (-1). isSigned() should return false because only non‑negative defined states are considered signed."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is Integer.MAX_VALUE (2,147,483,647). isSigned() should return false as it is outside the defined signed states."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is Integer.MIN_VALUE (-2,147,483,648). isSigned() should return false because it is not a recognized signed state."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is an unexpected positive value (6). isSigned() should return false, confirming that only the five defined constants are treated as signed."
        },
        {
            "input": [],
            "expected": true,
            "description": "After invoking setTSIG(TSIG key) the Message should transition to TSIG_SIGNED state; isSigned() must return true."
        },
        {
            "input": [],
            "expected": true,
            "description": "After invoking setTSIG(TSIG key, error, querytsig) with a non‑zero error that forces TSIG_FAILED, isSigned() must still return true because TSIG_FAILED is considered signed."
        }
    ]
}