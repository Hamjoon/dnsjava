{
  "tests": [
    {
      "group": "recordLength varying lengths - small values",
      "cases": [
        {
          "input": [],
          "expected": 55,
          "description": "TSIG constructed with name \"a.\" (wire‑format length 2) and algorithm \"alg.\" (wire‑format length 5). Expected length = 2 + 5 + 48 = 55."
        },
        {
          "input": [],
          "expected": 78,
          "description": "TSIG constructed with a name of length 10 (e.g., \"abcdefghij.\") and an algorithm name of length 20 (e.g., \"verylongalgorithmname.\"). Expected length = 10 + 20 + 48 = 78."
        }
      ]
    },
    {
      "group": "recordLength idempotent behavior",
      "cases": [
        {
          "input": [],
          "expected": 74,
          "description": "TSIG constructed with name \"example.com.\" and algorithm \"hmac-sha256.\" (both length 13). Calling recordLength() twice should return the same value 74 each time."
        }
      ]
    },
    {
      "group": "recordLength after mutable field change",
      "cases": [
        {
          "input": [],
          "expected": 81,
          "description": "Create a TSIG with name length 13 and algorithm length 13 (expected 74). Then, using reflection, change the `name` field to a Name whose wire‑format length is 20. recordLength() should now return 20 + 13 + 48 = 81."
        }
      ]
    },
    {
      "group": "recordLength overflow guard",
      "cases": [
        {
          "input": [],
          "expected": -2147483544,
          "description": "Using a mock Name that returns Integer.MAX_VALUE for length() and another mock Name returning 0 for algorithm length. The computed length overflows the int range: (2147483647 + 0 + 48) wraps to -2147483544. The method should return this wrapped value without throwing."
        }
      ]
    }
  ]
}