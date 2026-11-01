{
  "testCases": [
    {
      "group": "Message.setTSIG - extreme error codes",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-sha512\", \"bigkey\".getBytes())"
            },
            {
              "parameter": "error",
              "value": "Integer.MAX_VALUE"
            },
            {
              "parameter": "querytsig",
              "value": "null"
            }
          ],
          "expected": "Message.getTSIG() returns the same TSIG instance, Message.getGeneratedTSIG() returns null, and Message.tsigerror equals Integer.MAX_VALUE",
          "description": "Verify that the method stores the maximum positive int value without overflow or truncation."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "null"
            },
            {
              "parameter": "error",
              "value": "Integer.MIN_VALUE"
            },
            {
              "parameter": "querytsig",
              "value": "null"
            }
          ],
          "expected": "Message.getTSIG() returns null, Message.getGeneratedTSIG() returns null, and Message.tsigerror equals Integer.MIN_VALUE",
          "description": "Verify that the method stores the most negative int value correctly."
        }
      ]
    },
    {
      "group": "Message.setTSIG - empty algorithm and secret handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"\", new byte[0])"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "null"
            }
          ],
          "expected": "Message.getTSIG() returns the TSIG instance with empty algorithm and empty secret, Message.getGeneratedTSIG() returns null, and Message.tsigerror equals 0",
          "description": "Ensure that a TSIG with an empty algorithm string and an empty secret array is accepted and stored unchanged."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-md5\", new byte[0])"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "new TSIGRecord(Name.fromString(\"emptysecret.example.\"), DClass.IN, 300, new TSIG(\"hmac-md5\", new byte[0]))"
            }
          ],
          "expected": "Message.getTSIG() returns the TSIG with non‑empty algorithm but empty secret, Message.getGeneratedTSIG() returns the provided TSIGRecord (which also contains an empty secret), and Message.tsigerror equals 0",
          "description": "Check that a TSIGRecord containing a TSIG with an empty secret is stored correctly."
        }
      ]
    },
    {
      "group": "Message.setTSIG - zero and maximum TTL in query TSIG",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-sha256\", \"key\".getBytes())"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "new TSIGRecord(Name.fromString(\"zero.ttl.example.\"), DClass.IN, 0, new TSIG(\"hmac-sha256\", \"key\".getBytes()))"
            }
          ],
          "expected": "Message.getGeneratedTSIG() returns the TSIGRecord with TTL 0, all other fields unchanged",
          "description": "Validate that a TSIGRecord with a TTL of zero is accepted and stored without modification."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-sha256\", \"key\".getBytes())"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "new TSIGRecord(Name.fromString(\"max.ttl.example.\"), DClass.IN, Integer.MAX_VALUE, new TSIG(\"hmac-sha256\", \"key\".getBytes()))"
            }
          ],
          "expected": "Message.getGeneratedTSIG() returns the TSIGRecord with TTL Integer.MAX_VALUE, all other fields unchanged",
          "description": "Confirm that an extremely large TTL does not cause overflow or loss when stored."
        }
      ]
    },
    {
      "group": "Message.setTSIG - shared TSIG instance between key and query",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "sharedTsig"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "new TSIGRecord(Name.fromString(\"shared.example.\"), DClass.IN, 300, sharedTsig)"
            }
          ],
          "expected": "Message.getTSIG() and Message.getGeneratedTSIG().getAlgorithm() refer to the same TSIG instance (reference equality), Message.tsigerror equals 0",
          "description": "Test that when the same TSIG object is supplied both as the key and inside the TSIGRecord, the method stores the exact same reference for both fields."
        }
      ],
      "setup": [
        "TSIG sharedTsig = new TSIG(\"hmac-md5\", \"sharedSecret\".getBytes());"
      ]
    },
    {
      "group": "Message.setTSIG - clearing query TSIG on subsequent call",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-md5\", \"first\".getBytes())"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "new TSIGRecord(Name.fromString(\"first.example.\"), DClass.IN, 100, new TSIG(\"hmac-md5\", \"first\".getBytes()))"
            }
          ],
          "expected": "After first call, Message.getGeneratedTSIG() is non‑null",
          "description": "Initial set with a non‑null query TSIG."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-sha1\", \"second\".getBytes())"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "querytsig",
              "value": "null"
            }
          ],
          "expected": "After second call, Message.getGeneratedTSIG() returns null while Message.getTSIG() reflects the new key, confirming that a null querytsig overwrites the previous record.",
          "description": "Second invocation replaces the earlier query TSIG with null, testing that the method does not retain stale data."
        }
      ]
    }
  ]
}