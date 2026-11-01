{
  "group": "Message.isSigned()",
  "cases": [
    {
      "input": [],
      "expected": false,
      "description": "tsigState is TSIG_UNSIGNED (0). isSigned() should return false."
    },
    {
      "input": [],
      "expected": false,
      "description": "tsigState is TSIG_INTERMEDIATE (2). isSigned() should return false."
    },
    {
      "input": [],
      "expected": true,
      "description": "tsigState is TSIG_SIGNED (3). isSigned() should return true."
    },
    {
      "input": [],
      "expected": true,
      "description": "tsigState is TSIG_VERIFIED (1). isSigned() should return true."
    },
    {
      "input": [],
      "expected": true,
      "description": "tsigState is TSIG_FAILED (4). isSigned() should return true."
    },
    {
      "input": [],
      "expected": false,
      "description": "tsigState is an unexpected value (e.g., 5). isSigned() should return false."
    }
  ]
}