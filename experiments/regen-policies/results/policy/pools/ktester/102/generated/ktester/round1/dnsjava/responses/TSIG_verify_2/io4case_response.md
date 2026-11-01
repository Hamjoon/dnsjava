{
  "testCases": [
    {
      "group": "Exact fudge boundary (lower limit)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance containing a TSIG RR with algorithm HMAC_SHA256"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes that were signed"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord whose timestamp equals Instant.now() minus TSIG.FUDGE, correct MAC"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "0 (Rcode.NOERROR) – verification succeeds at the lower fudge boundary",
          "description": "The request timestamp is exactly now‑FUDGE; it should still be considered within the allowed window and verification must succeed."
        }
      ]
    },
    {
      "group": "Exact fudge boundary (upper limit)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance containing a TSIG RR with algorithm HMAC_SHA256"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes that were signed"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord whose timestamp equals Instant.now() plus TSIG.FUDGE, correct MAC"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "0 (Rcode.NOERROR) – verification succeeds at the upper fudge boundary",
          "description": "The request timestamp is exactly now+FUDGE; it should still be accepted and verification must succeed."
        }
      ]
    },
    {
      "group": "Empty raw message bytes",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a valid TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "new byte[0] (empty array)"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "valid TSIGRecord (algorithm HMAC_SHA1, correct MAC)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because the raw message is empty",
          "description": "An empty byte array cannot be used to compute the HMAC; the method should treat this as a malformed request."
        }
      ]
    },
    {
      "group": "Zero‑length MAC in TSIGRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm HMAC_SHA256 and mac = new byte[0]"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "16 (Rcode.BADSIG) – verification fails because MAC length is zero",
          "description": "A TSIG record that contains an empty MAC cannot be valid; verification must return BADSIG."
        }
      ]
    },
    {
      "group": "Supported algorithm HMAC_MD5 – successful verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR using algorithm HMAC_MD5"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes signed with HMAC‑MD5"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm HMAC_MD5, correct time, correct MAC"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "0 (Rcode.NOERROR) – verification succeeds for HMAC‑MD5",
          "description": "Ensures that the older HMAC‑MD5 algorithm is still recognised and verified correctly."
        }
      ]
    },
    {
      "group": "Mismatched secret key",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR signed using secret key A"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes signed with key A"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord built with the same algorithm but secret key B (different from the one used to sign)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "16 (Rcode.BADSIG) – verification fails because the MAC does not match the provided key",
          "description": "Even when algorithm and timestamp are correct, using a different secret should cause a bad‑signature result."
        }
      ]
    },
    {
      "group": "Message without TSIG RR but requestTSIG supplied",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance that does NOT contain a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "valid TSIGRecord (algorithm HMAC_SHA256, correct MAC)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because the message lacks the TSIG RR expected by the caller",
          "description": "The verifier expects a TSIG RR in the DNS message; its absence should be treated as a format error."
        }
      ]
    },
    {
      "group": "Null algorithm name in TSIGRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord whose algorithm field is null"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because algorithm cannot be resolved",
          "description": "A null algorithm name cannot be mapped to a MAC implementation, so the method must return FORMERR."
        }
      ]
    }
  ]
}