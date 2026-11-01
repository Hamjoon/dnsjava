{
  "testCases": [
    {
      "group": "apply_nullOld_fullSignature_true",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with no existing TSIG record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Message m now contains a TSIGRecord with error code Rcode.NOERROR and a full signature (MAC over the entire message). No exception is thrown.",
          "description": "Verify that when the old TSIGRecord is null, the method creates a new TSIGRecord using the default error code (NOERROR) and generates a full signature."
        }
      ]
    },
    {
      "group": "apply_nonNullOld_fullSignature_true",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a pre‑populated question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"testkey\", time = now)"
            }
          ],
          "expected": "Message m now contains a new TSIGRecord whose MAC is calculated using the key and algorithm from the provided old record, error code Rcode.NOERROR, and a full signature. The original old record is not modified.",
          "description": "When an existing TSIGRecord is supplied, the method must reuse its algorithm, key name and key data to sign the new message, producing a full signature."
        }
      ]
    },
    {
      "group": "apply_errorCode_nonZero_fullSignature_true",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance (any content)"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord"
            }
          ],
          "expected": "Message m now contains a TSIGRecord with error code set to Rcode.FORMERR (or any non‑zero error supplied via the overloaded method). The MAC is still calculated over the full message because fullSignature flag is true.",
          "description": "Although the public apply(Message, TSIGRecord) always passes Rcode.NOERROR, this test case documents the expected behaviour of the 4‑argument overload when a non‑zero error is supplied (full signature still generated)."
        }
      ]
    },
    {
      "group": "apply_errorCode_nonZero_fullSignature_false",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single answer record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord"
            }
          ],
          "expected": "Message m now contains a TSIGRecord with the supplied non‑zero error code and a truncated signature (only the first 10 bytes of the MAC) because fullSignature flag is false.",
          "description": "When fullSignature is false the implementation should generate a shortened MAC (as defined by the TSIG spec). This branch validates that behaviour."
        }
      ]
    },
    {
      "group": "apply_invalidAlgorithm_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm name \"unsupported-alg\""
            }
          ],
          "expected": "IllegalArgumentException (or GeneralSecurityException) is thrown indicating the algorithm is not supported.",
          "description": "The method should validate the algorithm name and reject unknown algorithms, raising an exception."
        }
      ]
    },
    {
      "group": "apply_keyTooShort_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with a key shorter than the minimum required length (e.g., 1 byte)"
            }
          ],
          "expected": "IllegalArgumentException is thrown because the secret key is too short for the selected HMAC algorithm.",
          "description": "Ensures the method validates key length before creating the MAC instance."
        }
      ]
    },
    {
      "group": "apply_clockSkewBeyondFudge_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord whose timeSigned is older than now‑FUDGE (e.g., now minus 10 minutes) and whose error code is Rcode.NOERROR"
            }
          ],
          "expected": "TSIGVerificationException (or a custom exception) is thrown indicating the timestamp is outside the allowed fudge window.",
          "description": "The apply method must reject signing when the supplied old record’s time is outside the acceptable +/- FUDGE range."
        }
      ]
    },
    {
      "group": "apply_messageWithExistingTSIG_replacesTSIG",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance that already contains a TSIGRecord (algorithm HMAC_SHA1, key \"oldkey\")"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"newkey\")"
            }
          ],
          "expected": "The original TSIGRecord in Message m is removed and replaced with a new TSIGRecord generated from the supplied old record (algorithm HMAC_SHA256, key \"newkey\"). No duplicate TSIG sections exist.",
          "description": "When a message already carries a TSIG, the method should replace it with the newly generated one based on the supplied old record."
        }
      ]
    },
    {
      "group": "apply_nullMessage_throwsNPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "null"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown because the Message argument is mandatory.",
          "description": "The method must guard against a null Message reference."
        }
      ]
    }
  ]
}