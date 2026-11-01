{
  "testCases": [
    {
      "group": "Empty MAC Handling",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message (valid, no TSIG variables)" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with correct algorithm, key, time, but mac = new byte[0]" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.BADSIG",
          "description": "An empty MAC (zero‑length signature) cannot match any computed MAC, so verification must fail with BADSIG."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message (valid)" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with mac = null" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.FORMERR",
          "description": "A null MAC field is considered malformed; the method should return FORMERR rather than attempting verification."
        }
      ]
    },
    {
      "group": "Timestamp Edge Fudge Window",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG time = now - 300 seconds" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with time = now - 300 seconds" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "Timestamp exactly at the negative edge of the allowed fudge window (‑300 s) is still acceptable."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG time = now + 300 seconds" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with time = now + 300 seconds" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "Timestamp exactly at the positive edge of the allowed fudge window (+300 s) is also accepted."
        }
      ]
    },
    {
      "group": "Full‑Signature with Matching Non‑Zero Error",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG error = 2 and other fields matching the MAC generation" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with error = 2, same algorithm/key/time as Message, and MAC generated including error=2" },
            { "parameter": "boolean fullSignature", "value": "true" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "When fullSignature is true the error field participates in MAC calculation; a matching non‑zero error value should verify successfully."
        }
      ]
    },
    {
      "group": "Algorithm Name Null or Empty",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with algorithm = null" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.FORMERR",
          "description": "A null algorithm string cannot be mapped; the method should treat the record as malformed and return FORMERR."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with algorithm = \"\"" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOTIMP",
          "description": "An empty algorithm string maps to no known Name constant; verification should return NOTIMP."
        }
      ]
    },
    {
      "group": "Empty Message Payload",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with no additional records (empty payload)" },
            { "parameter": "byte[] messageBytes", "value": "new byte[0]" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord whose MAC was generated over an empty payload" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "Verification must succeed when both the message and the MAC are computed over an empty byte array."
        }
      ]
    },
    {
      "group": "Key Name Not Found",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message (valid)" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with key name that is not present in the TSIG key store" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOTAUTH",
          "description": "If the TSIG key name cannot be resolved to a secret key, MAC initialization fails and the method should return NOTAUTH."
        }
      ]
    },
    {
      "group": "FullSignature false ignores error field",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG error = 5 (but fullSignature = false)" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with error = 5, MAC generated without the error field (as per false flag)" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "When fullSignature is false the error field must be ignored in MAC calculation; verification should succeed even though the error values differ."
        }
      ]
    }
  ]
}