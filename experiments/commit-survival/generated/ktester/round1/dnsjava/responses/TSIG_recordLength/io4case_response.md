{
  "testCases": [
    {
      "group": "Algorithm length variations",
      "cases": [
        {
          "input": [],
          "expected": 87,
          "description": "TSIG with name \"example.com.\" (length 13) and algorithm HMAC_MD5 (length 26). Expected record length = 13 + 26 + 48 = 87."
        },
        {
          "input": [],
          "expected": 71,
          "description": "TSIG with name \"example.com.\" (length 13) and algorithm GSS_TSIG (length 10). Expected record length = 13 + 10 + 48 = 71."
        },
        {
          "input": [],
          "expected": 72,
          "description": "TSIG with name \"example.com.\" (length 13) and algorithm HMAC_SHA1 (length 11). Expected record length = 13 + 11 + 48 = 72."
        },
        {
          "input": [],
          "expected": 77,
          "description": "TSIG with minimal name \"a.\" (length 3) and algorithm HMAC_MD5 (length 26). Expected record length = 3 + 26 + 48 = 77."
        }
      ]
    },
    {
      "group": "Edge name length cases",
      "cases": [
        {
          "input": [],
          "expected": 62,
          "description": "TSIG with root name \".\" (length 1) and algorithm HMAC_SHA256 (length 13). Expected record length = 1 + 13 + 48 = 62."
        },
        {
          "input": [],
          "expected": 59,
          "description": "TSIG with root name \".\" (length 1) and algorithm GSS_TSIG (length 10). Expected record length = 1 + 10 + 48 = 59."
        },
        {
          "input": [],
          "expected": 316,
          "description": "TSIG with a maximum‑size name (wire length 255) and algorithm HMAC_SHA256 (length 13). Expected record length = 255 + 13 + 48 = 316."
        }
      ]
    },
    {
      "group": "Combined null fields",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "TSIG constructed with both `name` and `alg` set to null. Calling recordLength() should throw NullPointerException because the method dereferences both fields."
        }
      ]
    },
    {
      "group": "Idempotent behavior",
      "cases": [
        {
          "input": [],
          "expected": 74,
          "description": "Calling recordLength() twice on the same TSIG instance (name \"example.com.\", algorithm HMAC_SHA256) returns the same value (74) each time, confirming the method is side‑effect free."
        }
      ]
    }
  ]
}