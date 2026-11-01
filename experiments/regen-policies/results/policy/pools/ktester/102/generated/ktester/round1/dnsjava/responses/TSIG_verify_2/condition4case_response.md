{
  "testCases": [
    {
      "group": "Successful verification (full signature)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a valid TSIG RR and matching MAC"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes that were signed"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm HMAC_SHA256, correct time, and correct MAC"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "0 (Rcode.NOERROR) – verification succeeds",
          "description": "When the request contains a TSIG record that matches the computed HMAC and the fullSignature flag is true, verify should return NOERROR."
        }
      ]
    },
    {
      "group": "Successful verification (partial signature)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a valid TSIG RR but without the additional signature fields"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes that were signed"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm HMAC_SHA1, correct time, and correct MAC"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "0 (Rcode.NOERROR) – verification succeeds",
          "description": "When fullSignature is false the method should verify only the base MAC; a matching MAC yields NOERROR."
        }
      ]
    },
    {
      "group": "Signature mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR whose MAC does not match the computed value"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm HMAC_SHA256, correct time, but altered MAC bytes"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "16 (Rcode.BADSIG) – verification fails due to bad signature",
          "description": "If the MAC stored in the TSIGRecord does not match the calculated MAC, verify should return BADSIG."
        }
      ]
    },
    {
      "group": "Bad time (outside fudge window)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with a timestamp older than now‑FUDGE or newer than now+FUDGE"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "18 (Rcode.BADTIME) – verification fails due to time skew",
          "description": "When the TSIG timestamp is outside the allowed fudge period, verify should return BADTIME."
        }
      ]
    },
    {
      "group": "Null requestTSIG",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance (any valid message)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "null"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because TSIG record is missing",
          "description": "If the requestTSIG argument is null, the method should treat the message as malformed and return FORMERR."
        }
      ]
    },
    {
      "group": "Null messageBytes",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance (any valid message)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "null"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "valid TSIGRecord"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because the raw message bytes are missing",
          "description": "When the raw message byte array is null, verify should return FORMERR."
        }
      ]
    },
    {
      "group": "Unsupported algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord whose algorithm name is not one of the known HMAC_* constants (e.g., \"unknown-alg.\")"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails because algorithm cannot be mapped to a MAC",
          "description": "If the algorithm specified in the TSIGRecord cannot be resolved to a known MAC algorithm, verify should return FORMERR."
        }
      ]
    },
    {
      "group": "Mac initialization exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte[] containing the raw DNS message bytes"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with a valid algorithm but an invalid secret key that causes Mac.getInstance to throw GeneralSecurityException"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "1 (Rcode.FORMERR) – verification fails due to cryptographic error",
          "description": "When initializing the Mac instance throws an exception (e.g., unsupported algorithm or bad key), verify should catch it and return FORMERR."
        }
      ]
    }
  ]
}