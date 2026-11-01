{
  "tests": [
    {
      "group": "Normal operation – simple query",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            }
          ],
          "expected": "byte[] whose length is <= 512 and contains a valid DNS wire format for a message with a single question record and no answer/authority/additional sections",
          "description": "A freshly created Message containing only a question Record (created via Message.newQuery(questionRecord)). The toWire(512) call should succeed and produce a wire‑format byte array that fits within the classic DNS UDP size limit."
        }
      ]
    },
    {
      "group": "Normal operation – message with multiple sections",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 4096
            }
          ],
          "expected": "byte[] whose length is <= 4096 and includes the header, question, answer, authority and additional sections in correct order",
          "description": "Message built with a question, two answer records, one authority record and one OPT (EDNS) record. toWire(4096) must serialize all sections and respect the supplied maxLength."
        }
      ]
    },
    {
      "group": "Maximum length handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 100
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Message contains enough records that the wire format would exceed 100 bytes. The method should detect the overflow and throw an IllegalArgumentException (or IOException wrapped as RuntimeException) indicating the message is too large for the supplied maxLength."
        },
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 65535
            }
          ],
          "expected": "byte[] whose length equals Message.MAXLENGTH (65535) when the message is deliberately filled to the maximum allowed size",
          "description": "Create a Message whose total wire size is exactly 65535 bytes (e.g., by adding many dummy records). toWire(Message.MAXLENGTH) must succeed and return a byte array of length 65535."
        }
      ]
    },
    {
      "group": "TSIG handling – unsigned message",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 1500
            }
          ],
          "expected": "byte[] without a TSIG record at the end and size field reflects only the DNS payload",
          "description": "Message has no TSIG set (tsigState == TSIG_UNSIGNED). toWire should serialize the message normally and not append any TSIG trailer."
        }
      ]
    },
    {
      "group": "TSIG handling – signed message",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 1500
            }
          ],
          "expected": "byte[] that ends with a TSIGRecord and whose total length does not exceed maxLength",
          "description": "Message is signed by calling setTSIG(key). The internal tsigState becomes TSIG_SIGNED. toWire must append the TSIG RR at the end of the wire format. The test verifies that the last 10‑+ bytes correspond to a TSIG RR and that the overall length respects the supplied maxLength."
        }
      ]
    },
    {
      "group": "TSIG handling – intermediate state (partial signing)",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 1500
            }
          ],
          "expected": "IllegalStateException",
          "description": "Message has tsigState == TSIG_INTERMEDIATE (e.g., after calling toWire(out) once but before final signing). Attempting a second toWire(maxLength) should fail because the message is in an inconsistent state."
        }
      ]
    },
    {
      "group": "Error conditions – null maxLength (negative value)",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": -1
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a negative maxLength is illegal; the method should immediately throw an IllegalArgumentException before attempting any serialization."
        }
      ]
    },
    {
      "group": "Error conditions – message with malformed header",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": 512
            }
          ],
          "expected": "IOException",
          "description": "Header fields (e.g., opcode, rcode) are set to values that are not permitted by the DNS spec. During serialization the method should raise an IOException indicating an invalid header."
        }
      ]
    }
  ]
}