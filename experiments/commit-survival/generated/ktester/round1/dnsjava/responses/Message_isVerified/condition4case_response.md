{
    "group": "Message.isVerified",
    "cases": [
        {
            "input": [],
            "expected": true,
            "description": "tsigState is set to TSIG_VERIFIED (1); isVerified() should return true."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is set to TSIG_UNSIGNED (0); isVerified() should return false."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is set to TSIG_INTERMEDIATE (2); isVerified() should return false."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is set to TSIG_SIGNED (3); isVerified() should return false."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is set to TSIG_FAILED (4); isVerified() should return false."
        },
        {
            "input": [],
            "expected": false,
            "description": "tsigState is set to an undefined value (e.g., 99); isVerified() should return false."
        }
    ]
}