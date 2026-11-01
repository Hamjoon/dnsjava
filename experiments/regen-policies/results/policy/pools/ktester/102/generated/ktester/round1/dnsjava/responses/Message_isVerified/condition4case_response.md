{
    "group": "Message.isVerified() - true branch",
    "cases": [
        {
            "input": [
                {
                    "parameter": "tsigState",
                    "value": 1
                }
            ],
            "expected": true,
            "description": "When the internal tsigState is set to TSIG_VERIFIED (1), isVerified() should return true."
        }
    ]
}