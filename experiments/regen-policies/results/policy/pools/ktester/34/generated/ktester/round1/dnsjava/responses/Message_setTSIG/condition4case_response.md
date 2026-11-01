{
  "testCases": [
    {
      "group": "setTSIG_nullKey",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "null"
            }
          ],
          "expected": "Message.isSigned() returns false; Message.getTSIG() returns null; internal tsigState equals Message.TSIG_UNSIGNED (0)",
          "description": "Calling setTSIG(null) should clear any existing TSIG information and leave the message unsigned."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "null"
            }
          ],
          "expected": "No exception is thrown; method returns normally.",
          "description": "Verify that passing a null TSIG does not cause a NullPointerException or any other runtime exception."
        }
      ]
    },
    {
      "group": "setTSIG_validKey",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-md5\", \"secret\".getBytes())"
            }
          ],
          "expected": "Message.isSigned() returns true; Message.getTSIG() returns a TSIGRecord whose key name matches the supplied TSIG; internal tsigState equals Message.TSIG_SIGNED (3)",
          "description": "When a non‑null TSIG key is supplied, the message should be marked as signed and a TSIGRecord should be generated."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-sha256\", new byte[0])"
            }
          ],
          "expected": "Message.isSigned() returns true; Message.getTSIG() is not null; tsigState == Message.TSIG_SIGNED (3)",
          "description": "Edge case: TSIG with an empty secret should still cause the message to be marked as signed (the library does not validate secret length at setTSIG time)."
        }
      ]
    },
    {
      "group": "setTSIG_multipleCalls",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(\"hmac-md5\", \"first\".getBytes())"
            }
          ],
          "expected": "After first call, isSigned() == true and tsigState == TSIG_SIGNED; after second call with a different key, isSigned() == true and tsigState == TSIG_SIGNED and the TSIGRecord reflects the second key.",
          "description": "Calling setTSIG twice should replace the previous TSIG information with the new one, without leaving the message in an intermediate or failed state."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "null"
            }
          ],
          "expected": "After a previous signed TSIG, calling setTSIG(null) clears the TSIG; isSigned() == false and tsigState == TSIG_UNSIGNED (0).",
          "description": "Ensure that a subsequent call with a null key resets the message to an unsigned state."
        }
      ]
    },
    {
      "group": "setTSIG_exceptionSafety",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "mock(TSIG.class) that throws IOException when used"
            }
          ],
          "expected": "Method does not propagate the IOException; it either logs the error and leaves the message unsigned (tsigState == TSIG_FAILED (4)) or throws a RuntimeException as defined by the library. The test asserts that no checked exception leaks.",
          "description": "Simulate a TSIG implementation that fails during internal processing to verify that setTSIG handles the failure gracefully."
        }
      ]
    }
  ]
}