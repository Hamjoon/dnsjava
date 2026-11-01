{
  "testCases": [
    {
      "group": "recordLength calculations - typical values",
      "cases": [
        {
          "input": [],
          "expected": 74,
          "description": "TSIG constructed with name \"example.com.\" and algorithm \"hmac-sha256.\". Both Name objects have a wire‑format length of 13 bytes. The method should compute (((((13+10)+13)+8)+18)+4)+8 = 74."
        },
        {
          "input": [],
          "expected": 59,
          "description": "TSIG constructed with the root name \".\" (length 1) and algorithm \"hmac-md5.\" (length 10). Expected length: (((((1+10)+10)+8)+18)+4)+8 = 59."
        },
        {
          "input": [],
          "expected": 558,
          "description": "TSIG constructed with a maximal DNS name (255 bytes) and a maximal algorithm name (255 bytes). Expected length: (((((255+10)+255)+8)+18)+4)+8 = 558."
        }
      ]
    },
    {
      "group": "recordLength null handling",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "TSIG instance where the `name` field is forcibly set to null via reflection. Calling recordLength() should throw a NullPointerException because `name.length()` is invoked on a null reference."
        },
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "TSIG instance where the `alg` field is forcibly set to null via reflection. Calling recordLength() should throw a NullPointerException because `alg.length()` is invoked on a null reference."
        }
      ]
    }
  ]
}