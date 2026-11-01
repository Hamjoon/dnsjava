{
  "testCases": [
    {
      "group": "Zero maxLength without truncation (exception expected)",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 0
            },
            {
              "parameter": "truncate",
              "value": false
            }
          ],
          "expected": "MessageSizeExceededException is thrown with maxLength = 0; internal size field is not updated",
          "description": "A maxLength of 0 cannot accommodate even the DNS header, so toWire(out, 0) returns false. Because truncate is false the method must throw MessageSizeExceededException."
        }
      ]
    },
    {
      "group": "Zero maxLength with truncation flag true",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 0
            },
            {
              "parameter": "truncate",
              "value": true
            }
          ],
          "expected": "returns a non‑null byte[] of length 0 and no exception is thrown; internal size field is set to 0",
          "description": "When truncate is true the method suppresses the exception even though the message cannot be rendered; it returns the (empty) truncated wire representation."
        }
      ]
    },
    {
      "group": "Maximum allowed maxLength without truncation",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 65535
            },
            {
              "parameter": "truncate",
              "value": false
            }
          ],
          "expected": "returns a non‑null byte[] whose length ≤ 65535 and no exception is thrown; internal size field equals the returned array length",
          "description": "Using the protocol‑defined maximum length ensures the message can always be completely rendered; the method should succeed and record the correct size."
        }
      ]
    },
    {
      "group": "Maximum allowed maxLength with truncation flag true",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 65535
            },
            {
              "parameter": "truncate",
              "value": true
            }
          ],
          "expected": "returns a non‑null byte[] whose length ≤ 65535 and no exception is thrown; internal size field equals the returned array length",
          "description": "Even when truncate is true the message fits, so the method behaves like the normal case and records the correct size."
        }
      ]
    },
    {
      "group": "Size field consistency on complete render",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            },
            {
              "parameter": "truncate",
              "value": false
            }
          ],
          "expected": "size field of the Message instance equals the length of the returned byte[] (both ≤ 512)",
          "description": "When the message is completely rendered the method sets the private `size` field to the number of bytes actually written; the test verifies that `size` matches the output length."
        }
      ]
    },
    {
      "group": "Size field consistency on truncated render",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 128
            },
            {
              "parameter": "truncate",
              "value": true
            }
          ],
          "expected": "size field of the Message instance equals the length of the returned byte[] (both ≤ 128)",
          "description": "When truncation is allowed and the message does not fit, the method returns a truncated wire format and records the truncated length in the `size` field."
        }
      ]
    },
    {
      "group": "Negative maxLength handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": -1
            },
            {
              "parameter": "truncate",
              "value": false
            }
          ],
          "expected": "MessageSizeExceededException is thrown with maxLength = -1",
          "description": "A negative maxLength is invalid; the private `toWire` will treat it as insufficient space, causing the method to throw MessageSizeExceededException when truncate is false."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": -1
            },
            {
              "parameter": "truncate",
              "value": true
            }
          ],
          "expected": "returns a non‑null byte[] of length 0 and no exception is thrown; internal size field is set to 0",
          "description": "With truncate=true the method suppresses the exception even for a negative maxLength and returns an empty (truncated) wire representation."
        }
      ]
    }
  ]
}