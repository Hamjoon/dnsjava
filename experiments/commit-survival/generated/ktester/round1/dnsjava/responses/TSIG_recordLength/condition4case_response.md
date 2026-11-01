{
  "testCases": [
    {
      "group": "Normal behavior",
      "cases": [
        {
          "input": [],
          "expected": 74,
          "description": "TSIG with standard name \"example.com.\" and algorithm HMAC_SHA256. Expected record length = name.length() + alg.length() + 48 = 13 + 13 + 48 = 74."
        },
        {
          "input": [],
          "expected": 64,
          "description": "TSIG with minimal name \"a.\" (length 3) and algorithm HMAC_SHA256 (length 13). Expected record length = 3 + 13 + 48 = 64."
        },
        {
          "input": [],
          "expected": 102,
          "description": "TSIG with a long name \"this.is.a.very.long.domain.name.example.\" (length 41) and algorithm HMAC_SHA256 (length 13). Expected record length = 41 + 13 + 48 = 102."
        }
      ]
    },
    {
      "group": "Exception cases",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "TSIG constructed with a null `name` field. Calling recordLength() should throw NullPointerException because the method dereferences `name.length()`."
        },
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "TSIG constructed with a null `alg` field. Calling recordLength() should throw NullPointerException because the method dereferences `alg.length()`."
        }
      ]
    }
  ]
}