{
  "testCases": [
    {
      "group": "apply(Message, TSIGRecord) - Existing TSIG with Non‑null Old Record (different key)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that already contains a TSIG RR signed with key name \"keyA\" and algorithm HMAC‑SHA256"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord signed with a different key name \"keyB\", same algorithm HMAC‑SHA256, and a valid MAC"
            }
          ],
          "expected": "The original TSIG RR is removed and replaced by a new TSIG RR generated using the key and algorithm from the supplied old record (keyB, HMAC‑SHA256). The message ends with exactly one TSIG RR and no exception is thrown.",
          "description": "When the message already carries a TSIG but a non‑null old record is supplied, the method must discard the existing TSIG and create a fresh one based on the old record’s key and algorithm, even if the keys differ."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Old Record using HMAC‑MD5 algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a single question section (type A) and no TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord whose algorithm name is HMAC‑MD5 (Name HMAC_MD5), with a valid key and MAC"
            }
          ],
          "expected": "A TSIG RR is added to the message using the HMAC‑MD5 algorithm; the MAC is correctly computed and the message contains exactly one TSIG RR. No exception is thrown.",
          "description": "The overload must correctly map the legacy HMAC‑MD5 algorithm name to the corresponding MAC implementation and generate a valid TSIG record."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Old Record with Future Timestamp beyond fudge",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Empty Message (no sections) with no TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord whose timeSigned is current system time + 10 minutes (exceeds FUDGE of 300 s)"
            }
          ],
          "expected": "SecurityException (or a subclass of GeneralSecurityException) indicating that the TSIG timestamp is outside the allowed fudge window.",
          "description": "If the supplied old TSIGRecord has a timeSigned value that is too far in the future, the method should reject it to prevent replay attacks."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Old Record with Zero‑Length MAC",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message containing a query section and no TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with a valid key and algorithm but an empty MAC byte array"
            }
          ],
          "expected": "Message m receives a new TSIG RR; the MAC is computed using the supplied key/algorithm and the empty old MAC is treated as a zero‑length value without causing an exception.",
          "description": "The method should handle an old TSIGRecord that has an empty MAC field gracefully, using it as part of the MAC calculation per the TSIG spec."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Message with Full DNS Sections (query, answer, authority, additional)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that contains a question, one answer RR, one authority RR, and one additional RR (no existing TSIG)"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "A TSIG RR is appended to the message; the MAC is calculated over the complete DNS payload (all sections) plus the TSIG pseudo‑header. The resulting TSIG uses the default algorithm (HMAC‑SHA256) and Rcode.NOERROR.",
          "description": "Ensures that the apply method correctly incorporates all DNS sections when generating the MAC for a full‑featured message."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Message containing an OPT (EDNS0) record",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a standard query section and an OPT record (EDNS0) in the additional section, no existing TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "A TSIG RR is added; the MAC calculation includes the OPT record as part of the additional section, complying with the TSIG specification for EDNS0. No exception is thrown.",
          "description": "Verifies that the presence of an OPT record does not break TSIG generation and that the OPT RR is correctly incorporated into the MAC."
        }
      ]
    }
  ]
}