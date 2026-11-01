{
  "group": "Message.isSigned",
  "cases": [
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 0
        }
      ],
      "expected": false,
      "description": "tsigState is TSIG_UNSIGNED (0); isSigned() should return false."
    },
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 2
        }
      ],
      "expected": false,
      "description": "tsigState is TSIG_INTERMEDIATE (2); isSigned() should return false."
    },
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 3
        }
      ],
      "expected": true,
      "description": "tsigState is TSIG_SIGNED (3); isSigned() should return true."
    },
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 1
        }
      ],
      "expected": true,
      "description": "tsigState is TSIG_VERIFIED (1); isSigned() should return true."
    },
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 4
        }
      ],
      "expected": true,
      "description": "tsigState is TSIG_FAILED (4); isSigned() should return true."
    },
    {
      "input": [
        {
          "parameter": "tsigState",
          "value": 99
        }
      ],
      "expected": false,
      "description": "tsigState is an undefined value (99); isSigned() should return false, ensuring unknown states are treated as unsigned."
    }
  ]
}