{
  "tests": [
    {
      "group": "apply_futureTimestampWithinFudge",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single question record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"testkey\", timeSigned = now + FUDGE - 1 second, error = Rcode.NOERROR)"
            }
          ],
          "expected": "A new TSIGRecord is added to the message, the signature is calculated, and no exception is thrown. The timestamp of the generated TSIGRecord is the current time (within the allowed fudge window).",
          "description": "When the supplied old TSIGRecord’s timeSigned is in the future but still inside the allowed ±FUDGE window, apply should accept it and produce a valid full‑signature TSIG."
        }
      ]
    },
    {
      "group": "apply_futureTimestampBeyondFudge",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single question record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"testkey\", timeSigned = now + FUDGE + 1 second, error = Rcode.NOERROR)"
            }
          ],
          "expected": "TSIGVerificationException (or IllegalArgumentException) is thrown indicating the timestamp is outside the allowed fudge window.",
          "description": "If the old TSIGRecord’s timeSigned is more than FUDGE seconds in the future, the apply method must reject the operation."
        }
      ]
    },
    {
      "group": "apply_algorithmHMAC_MD5",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance containing a question and an answer section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_MD5, key \"md5key\", timeSigned = now, error = Rcode.NOERROR)"
            }
          ],
          "expected": "Message m now contains a TSIGRecord signed with HMAC‑MD5. The MAC length is 16 bytes and no exception is thrown.",
          "description": "Verify that the apply method correctly handles the legacy HMAC‑MD5 algorithm and produces a full‑signature of the expected length."
        }
      ]
    },
    {
      "group": "apply_keyExactMinimumLength_SHA256",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single answer record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key = 16‑byte array {0x00..0x0F}, timeSigned = now, error = Rcode.NOERROR)"
            }
          ],
          "expected": "A TSIGRecord is added; the MAC is successfully generated (32‑byte length) and no exception is thrown.",
          "description": "The minimum key size for HMAC‑SHA256 is 16 bytes. This test ensures that a key of exactly that length is accepted."
        }
      ]
    },
    {
      "group": "apply_oldRecordImmutability",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Empty Message instance (no sections)"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA1, key \"immutable\", timeSigned = now, error = Rcode.NOERROR, MAC = {0xAA,0xBB})"
            }
          ],
          "expected": "Message receives a new TSIGRecord; the original TSIGRecord passed as 'old' remains unchanged (same key, algorithm, timeSigned and MAC).",
          "description": "apply must not mutate the supplied TSIGRecord; it should be treated as read‑only."
        }
      ]
    },
    {
      "group": "apply_repeatApplyDifferentSignature",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single question record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"repeatkey\", timeSigned = now, error = Rcode.NOERROR)"
            }
          ],
          "expected": "Two successive calls to apply on the same Message (after clearing the previous TSIG) produce TSIGRecords with different timestamps and therefore different MAC values.",
          "description": "Because the signature includes the current time, invoking apply twice should yield distinct MACs even when the message content and key are identical."
        }
      ]
    },
    {
      "group": "apply_emptyMessageFullSignatureLength",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with no question, answer, authority or additional records"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA384, key \"emptymsgkey\", timeSigned = now, error = Rcode.NOERROR)"
            }
          ],
          "expected": "A TSIGRecord is added; the generated MAC length is 48 bytes (full HMAC‑SHA384 output) and no exception is thrown.",
          "description": "Even when the DNS message is empty, apply must still compute a full‑signature MAC of the correct length for the selected algorithm."
        }
      ]
    }
  ]
}