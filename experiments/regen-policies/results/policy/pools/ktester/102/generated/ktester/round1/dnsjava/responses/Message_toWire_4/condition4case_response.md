{
  "testCases": [
    {
      "group": "Completely rendered without truncation",
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
          "expected": "returns a non‑null byte[] whose length ≤ 512 and no exception is thrown",
          "description": "The message fits within the supplied maxLength, so the private toWire(out, maxLength) returns true. Because the message is completely rendered, the method should simply set the internal size field and return the wire format."
        }
      ]
    },
    {
      "group": "Completely rendered with truncation flag true",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            },
            {
              "parameter": "truncate",
              "value": true
            }
          ],
          "expected": "returns a non‑null byte[] (may be truncated) and no exception is thrown",
          "description": "Even though the message is completely rendered, the truncate flag is true. The branch `if ((!completelyRendered) && (!truncate))` is not taken, so the method behaves like the normal case."
        }
      ]
    },
    {
      "group": "Partially rendered without truncation (exception expected)",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 128
            },
            {
              "parameter": "truncate",
              "value": false
            }
          ],
          "expected": "MessageSizeExceededException is thrown with maxLength = 128",
          "description": "The message is larger than maxLength, so the private toWire(out, maxLength) returns false (not completely rendered). Because truncate is false, the method must throw MessageSizeExceededException."
        }
      ]
    },
    {
      "group": "Partially rendered with truncation flag true",
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
          "expected": "returns a non‑null byte[] (truncated to ≤ 128 bytes) and no exception is thrown",
          "description": "The message does not fit into maxLength, so toWire returns false, but because truncate is true the exception is suppressed. The method should still set size and return the truncated wire representation."
        }
      ]
    }
  ]
}