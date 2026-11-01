{
  "testCases": [
    {
      "group": "Null and Invalid Parameter Checks",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "null" },
            { "parameter": "byte[] messageBytes", "value": "new byte[0]" },
            { "parameter": "TSIGRecord requestTSIG", "value": "null" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.FORMERR (or appropriate error code for missing TSIG record)",
          "description": "Verify that the method returns a format error when the TSIG record argument is null."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message (valid)" },
            { "parameter": "byte[] messageBytes", "value": "null" },
            { "parameter": "TSIGRecord requestTSIG", "value": "mock TSIGRecord (valid)" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.FORMERR (or appropriate error code for missing message bytes)",
          "description": "Verify that a null messageBytes array results in a format error."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "null" },
            { "parameter": "byte[] messageBytes", "value": "null" },
            { "parameter": "TSIGRecord requestTSIG", "value": "null" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.FORMERR",
          "description": "All inputs null – the method should fail fast with a format error."
        }
      ]
    },
    {
      "group": "Algorithm Name Mapping",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with algorithm = \"HMAC-MD5.SIG-ALG.REG.INT.\"" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR (or successful verification path)",
          "description": "Valid algorithm name that maps to a known Name constant; should follow the normal verification flow."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "mock Message" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with algorithm = \"UNKNOWN-ALG.\"" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOTIMP (or appropriate error for unsupported algorithm)",
          "description": "Algorithm string that does not map to any known Name constant; method should return NOTIMP."
        }
      ]
    },
    {
      "group": "Time‑Fudge Validation",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG time = now - 100 seconds" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with time = now - 100 seconds" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "TSIG timestamp within the allowed fudge window (±300 s); verification should succeed."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG time = now - 400 seconds" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with time = now - 400 seconds" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.BADTIME",
          "description": "TSIG timestamp older than the allowed fudge; method should return BADTIME."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "Message with TSIG time = now + 400 seconds" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with time = now + 400 seconds" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.BADTIME",
          "description": "TSIG timestamp in the future beyond the fudge window; method should return BADTIME."
        }
      ]
    },
    {
      "group": "Signature Verification",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with correct MAC" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with matching signature" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "When the MAC computed over the message matches the TSIG signature, verification should succeed."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "Message with altered payload" },
            { "parameter": "byte[] messageBytes", "value": "alteredMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with signature from original payload" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.BADSIG",
          "description": "If the computed MAC does not match the TSIG signature, the method should return BADSIG."
        }
      ]
    },
    {
      "group": "Full‑Signature Flag Paths",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with additional TSIG variables (e.g., error, other data)" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with error = 0" },
            { "parameter": "boolean fullSignature", "value": "true" }
          ],
          "expected": "Rcode.NOERROR",
          "description": "When fullSignature is true, the method includes extra fields (error, other data) in the MAC calculation; with correct data verification succeeds."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "Message with mismatched error field" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with error = 1 (but MAC was generated with error = 0)" },
            { "parameter": "boolean fullSignature", "value": "true" }
          ],
          "expected": "Rcode.BADSIG",
          "description": "Full‑signature mode but the error field used for MAC generation differs; verification must fail with BADSIG."
        }
      ]
    },
    {
      "group": "Exception Handling (Mac initialization failure)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "Message with algorithm = \"HMAC-MD5.SIG-ALG.REG.INT.\"" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes" },
            { "parameter": "TSIGRecord requestTSIG", "value": "TSIGRecord with key that triggers GeneralSecurityException" },
            { "parameter": "boolean fullSignature", "value": "false" }
          ],
          "expected": "Rcode.NOTAUTH (or appropriate error indicating MAC init failure)",
          "description": "If creating the Mac instance throws a GeneralSecurityException (e.g., unsupported algorithm), the method should catch it and return NOTAUTH."
        }
      ]
    }
  ]
}