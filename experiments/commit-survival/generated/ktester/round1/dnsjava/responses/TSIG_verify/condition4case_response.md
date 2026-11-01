Invalid control character at: line 12 column 27 (char 401)
{
  "testCases": [
    {
      "group": "Successful verification (full signature)",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordMatchingSignature" }
          ],
          "expected": "0   // Rcode.NOERROR – verification succeeded",
          "description": "All inputs are correct: the TSIG record matches the HMAC of the supplied message bytes, the algorithm is supported, and the timestamp is within the default FUDGE window. The method should return NOERROR."
        }
      ]
    },
    {
      "group": "Signature mismatch",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordWithWrongSignature" }
          ],
          "expected": "16   // Rcode.BADTIME – HMAC verification failed",
          "description": "The TSIG record is present but its signature does not match the HMAC calculated over the message bytes. The method should detect the mismatch and return a failure code (e.g., BADTIME or BADSIG)."
        }
      ]
    },
    {
      "group": "Timestamp outside fudge window",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordWithOldTimestamp" }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp is too far from current time",
          "description": "The TSIG record contains a timestamp that is older than the current time minus the FUDGE duration (300 s). Verification should fail with BADTIME."
        },
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordWithFutureTimestamp" }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp is too far in the future",
          "description": "The TSIG record contains a timestamp that is newer than the current time plus the FUDGE duration. Verification should fail with BADTIME."
        }
      ]
    },
    {
      "group": "Unsupported algorithm",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordWithUnknownAlgorithm" }
          ],
          "expected": "16   // Rcode.NOTIMP – algorithm not implemented",
          "description": "The TSIG record specifies an algorithm name that TSIG does not recognise (e.g., \"hmac-foo\"). The method should return NOTIMP."
        }
      ]
    },
    {
      "group": "Null or missing TSIG record",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithoutTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "null" }
          ],
          "expected": "0   // Rcode.NOERROR – no TSIG to verify, treat as success",
          "description": "When the requestTSIG argument is null, the method should consider that no TSIG verification is required and return NOERROR."
        }
      ]
    },
    {
      "group": "Invalid message bytes (null)",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "null" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordMatchingSignature" }
          ],
          "expected": "1   // Rcode.FORMERR – malformed input",
          "description": "If the raw message byte array is null, the method cannot compute the HMAC and should return FORMERR."
        }
      ]
    },
    {
      "group": "GSS‑TSIG special handling",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithGssTsig" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validGssTsigRecord" }
          ],
          "expected": "0   // Rcode.NOERROR – GSS‑TSIG verification succeeded",
          "description": "When the algorithm name equals TSIG.GSS_TSIG, the verification path uses a different MAC (e.g., Kerberos). Assuming the mock GSS‑TSIG record is correctly signed, the method should return NOERROR."
        }
      ]
    },
    {
      "group": "Full‑signature flag effect (internal path)",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "validMessageWithTSIG" },
            { "parameter": "messageBytes", "value": "validMessageBytes" },
            { "parameter": "requestTSIG", "value": "validTSIGRecordMatchingSignature" }
          ],
          "expected": "0   // Rcode.NOERROR – same result as public overload",
          "description": "The public overload `verify(m, bytes, requestTSIG)` forwards to the private method with `fullSignature=true`. The outcome should be identical to calling the 4‑argument version directly with `fullSignature=true`."
        }
      ]
    }
  ]
}