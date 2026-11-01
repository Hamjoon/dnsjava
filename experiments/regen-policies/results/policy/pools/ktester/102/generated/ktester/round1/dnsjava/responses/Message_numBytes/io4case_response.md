{
  "tests": [
    {
      "group": "Reflection manipulation",
      "cases": [
        {
          "input": [
            {
              "parameter": "sizeValue",
              "value": 12345
            }
          ],
          "expected": 12345,
          "description": "Set the private field `size` to a positive value using reflection and verify that `numBytes()` returns the same value."
        },
        {
          "input": [
            {
              "parameter": "sizeValue",
              "value": -1
            }
          ],
          "expected": -1,
          "description": "Force the private `size` field to a negative number via reflection and ensure `numBytes()` returns the negative value (testing that the method does not enforce non‑negative constraints itself)."
        }
      ]
    },
    {
      "group": "Boundary values",
      "cases": [
        {
          "input": [
            {
              "parameter": "sizeValue",
              "value": 0
            }
          ],
          "expected": 0,
          "description": "Explicitly set `size` to the lower boundary (0) using reflection and confirm `numBytes()` returns 0."
        },
        {
          "input": [
            {
              "parameter": "sizeValue",
              "value": 65535
            }
          ],
          "expected": 65535,
          "description": "Set `size` to the upper allowed DNS message length (Message.MAXLENGTH) via reflection and verify `numBytes()` returns 65535."
        }
      ]
    },
    {
      "group": "Effect of toWire on size",
      "cases": [
        {
          "input": [],
          "expected": 0,
          "description": "Create a new empty Message, invoke `toWire()` (which serializes the message) and verify that `numBytes()` still returns 0 because no records have been added."
        },
        {
          "input": [
            {
              "parameter": "recordCount",
              "value": 1
            }
          ],
          "expected": ">= 12",
          "description": "Add a simple DNS record (e.g., an A record) to the Message, call `toWire()`, and assert that `numBytes()` returns a value at least equal to the minimal DNS wire format size (header 12 bytes). The exact value depends on the record content but must be non‑zero."
        }
      ]
    }
  ]
}