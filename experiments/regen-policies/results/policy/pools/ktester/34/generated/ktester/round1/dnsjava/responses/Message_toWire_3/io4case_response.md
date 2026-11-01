{
  "testCases": [
    {
      "group": "Exact fit – maxLength equals required wire size",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 28
            }
          ],
          "expected": "byte[] length == 28 and Message.size == 28",
          "description": "A minimal query Message (header + one question with name \"example.com.\") requires exactly 28 bytes. Supplying maxLength = 28 should succeed, returning a byte array of length 28 and setting the internal size field accordingly."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 12
            }
          ],
          "expected": "IOException",
          "description": "A Message always contains at least a 12‑byte DNS header. Providing maxLength equal to the header size (12) leaves no room for the question section, so the private toWire should detect overflow and throw an IOException."
        }
      ]
    },
    {
      "group": "Message with TSIG – maxLength just sufficient for signed message",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 64
            }
          ],
          "expected": "byte[] length == 64 and Message.size == 64",
          "description": "Create a Message with a question and a TSIG record (typical signed DNS query). The total wire size is 64 bytes. Supplying maxLength = 64 should allow the full message to be serialized without exception."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 63
            }
          ],
          "expected": "IOException",
          "description": "Using the same signed Message but with maxLength one byte short (63) must cause the serializer to abort and throw an IOException because the TSIG cannot be fully written."
        }
      ]
    },
    {
      "group": "Message with OPT (EDNS0) – insufficient maxLength",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 20
            }
          ],
          "expected": "IOException",
          "description": "Add an OPT record (EDNS0) to a Message; the OPT RR alone exceeds 20 bytes. The method should detect that the buffer cannot accommodate the OPT section and raise an IOException."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 40
            }
          ],
          "expected": "byte[] length == 40 and Message.size == 40",
          "description": "Provide a maxLength that exactly matches the total wire size of the Message with the OPT record (40 bytes). Serialization should succeed and the returned array length must equal the supplied limit."
        }
      ]
    },
    {
      "group": "Multiple invocations – size field reflects latest serialization",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            }
          ],
          "expected": "Message.size == firstByteArray.length",
          "description": "First call to toWire(512) on a simple query. Verify that size equals the length of the returned array."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 1024
            }
          ],
          "expected": "Message.size == secondByteArray.length && secondByteArray.length > firstByteArray.length",
          "description": "After adding an additional answer record to the same Message, call toWire(1024). The size field must be updated to the new length, which should be larger than the length from the previous call."
        }
      ]
    },
    {
      "group": "Empty Message – only DNS header present",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 12
            }
          ],
          "expected": "byte[] length == 12 and Message.size == 12",
          "description": "Create a Message with no question and no records. The wire format consists solely of the 12‑byte DNS header. Supplying maxLength = 12 should succeed and produce exactly 12 bytes."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 11
            }
          ],
          "expected": "IOException",
          "description": "Attempt to serialize the same empty Message with maxLength smaller than the mandatory header size (11). The method must throw an IOException because the header cannot fit."
        }
      ]
    },
    {
      "group": "Extreme integer limits for maxLength",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": -2147483648
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing Integer.MIN_VALUE as maxLength should be treated as an invalid negative argument and cause an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 2147483647
            }
          ],
          "expected": "byte[] length ≤ 65535 and Message.size == returnedByteArray.length",
          "description": "A very large positive maxLength (Integer.MAX_VALUE) is beyond Message.MAXLENGTH. The implementation should cap the limit to 65535 and serialize the message accordingly without throwing."
        }
      ]
    }
  ]
}