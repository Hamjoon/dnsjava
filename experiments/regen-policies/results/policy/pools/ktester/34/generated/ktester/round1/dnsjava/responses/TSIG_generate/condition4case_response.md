{
  "testCases": [
    {
      "group": "Null and Invalid Inputs",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "null" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01,0x02}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "The method should reject a null Message and throw NPE before any processing."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "null" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "A null byte array must cause NPE because the MAC calculation needs the original wire format."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[0]" },
            { "parameter": "int error", "value": "-1" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "Negative error codes are invalid; the implementation should validate and throw IAE."
        }
      ]
    },
    {
      "group": "Error Code Branches",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x10,0x20}" },
            { "parameter": "int error", "value": "Rcode.BADTIME" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error field set to BADTIME",
          "description": "When the error code indicates a bad time, generate must embed that error in the returned TSIGRecord."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x10,0x20}" },
            { "parameter": "int error", "value": "Rcode.BADSIG" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error field set to BADSIG",
          "description": "When the error code indicates a bad signature, generate must embed that error in the returned TSIGRecord."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x10,0x20}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error field set to 0 (NOERROR)",
          "description": "A normal (no‑error) invocation should produce a TSIGRecord whose error field is 0."
        }
      ]
    },
    {
      "group": "FullSignature Variations",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x55,0x66,0x77}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose MAC length equals the full output length of the selected HMAC algorithm",
          "description": "When fullSignature is true (default), generate must return the complete MAC."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x55,0x66,0x77}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose MAC is truncated to 10 bytes (or the algorithm‑specific truncation length)",
          "description": "When fullSignature is false, generate must truncate the MAC according to the RFC‑2845 rules."
        }
      ]
    },
    {
      "group": "Old Record Influence",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01,0x02,0x03}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) with prior MAC and time" }
          ],
          "expected": "TSIGRecord whose time field is derived from the old record (time signed + FUDGE) and MAC incorporates the previous MAC",
          "description": "If an old TSIGRecord is supplied, the new MAC must be calculated over the previous MAC and the new message, per the TSIG chaining rules."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01,0x02,0x03}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose time field equals the current Instant (within a few milliseconds) and MAC does not include a prior MAC",
          "description": "When old is null, the method should start a fresh TSIG chain using the current time."
        }
      ]
    },
    {
      "group": "Algorithm Selection",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with TSIG algorithm HMAC_SHA256" },
            { "parameter": "byte[] b", "value": "new byte[]{0x0A,0x0B}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose name field is HMAC_SHA256 and MAC length matches SHA‑256 output (32 bytes)",
          "description": "The generate method must map the algorithm string to the correct Name constant and use the matching HMAC algorithm."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with TSIG algorithm \"unknown-alg\"" },
            { "parameter": "byte[] b", "value": "new byte[]{0x0A,0x0B}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "An unsupported algorithm name should cause the method to reject the request with an IAE."
        }
      ]
    },
    {
      "group": "Clock / Time Fudge Handling",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose time value is within ±TSIG.FUDGE of the system Clock instant",
          "description": "The generated TSIG timestamp must be the current time; the test verifies it is not older than now‑FUDGE and not newer than now+FUDGE."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message)" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) with time = Instant.now().minusSeconds(600)" }
          ],
          "expected": "TSIGRecord with error field set to Rcode.BADTIME",
          "description": "If the old record’s time is outside the allowed fudge window, generate must return a TSIGRecord indicating BADTIME."
        }
      ]
    }
  ]
}