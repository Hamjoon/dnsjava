{
  "test_cases": [
    {
      "group": "Null Parameters",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "null" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown because Message is null",
          "description": "Calling apply with a null Message should be rejected immediately."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "Message contains a TSIGRecord with algorithm HMAC_SHA256 and error code 0",
          "description": "When old TSIGRecord is null the method should create a fresh TSIGRecord using the default algorithm."
        }
      ]
    },
    {
      "group": "Error Code Handling",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "9" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "Message contains a TSIGRecord whose error field equals 9 (BADTIME)",
          "description": "The method must propagate the supplied error code into the generated TSIGRecord."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "-1" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "IllegalArgumentException thrown because error code is out of range",
          "description": "Negative error codes are invalid and should cause an exception."
        }
      ]
    },
    {
      "group": "Full Signature Flag",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord added with a full MAC (signature) covering the whole message",
          "description": "When fullSignature defaults to true, the MAC must be calculated over the entire DNS message."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord added with a truncated MAC (only the first 10 bytes) because fullSignature is false",
          "description": "When the overloaded apply(..., false) is used, the signature should be shortened according to the DNS TSIG spec."
        }
      ]
    },
    {
      "group": "Old Record Reuse",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "TSIGRecord with keyName='example.com.', algorithm=HMAC_SHA256, time=Instant.now()" }
          ],
          "expected": "New TSIGRecord re‑uses the key name and algorithm from the old record and updates the time field",
          "description": "If an old TSIGRecord is supplied, the new record must inherit its key name and algorithm while refreshing the timestamp."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "TSIGRecord with algorithm=HMAC_SHA1, keyName='example.org.', time=Instant.now().minusSeconds(600)" }
          ],
          "expected": "IllegalArgumentException thrown because the old record is older than the allowed FUDGE (300 s)",
          "description": "The method must reject an old TSIGRecord whose timestamp is outside the acceptable time window."
        }
      ]
    },
    {
      "group": "Algorithm Name Mapping",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "TSIGRecord with algorithm name='HMAC-MD5.SIG-ALG.REG.INT.'" }
          ],
          "expected": "TSIGRecord uses the MD5 algorithm for MAC generation",
          "description": "Verify that algorithmToName correctly resolves the MD5 constant and the MAC is generated with HMAC‑MD5."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "new Message()" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "TSIGRecord with algorithm name='unknown-alg'" }
          ],
          "expected": "IllegalArgumentException thrown because algorithm name cannot be mapped to a known Name",
          "description": "When an unsupported algorithm string is supplied the method should fail fast."
        }
      ]
    }
  ]
}