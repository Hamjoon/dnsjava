{
  "testCases": [
    {
      "group": "setTSIG_unicodeKeyName",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"ñame.example.\"), \"hmac-sha256\", \"unicodeSecret\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "Message.isSigned() returns true; Message.getTSIG() returns a TSIGRecord whose name is the Unicode name (\"ñame.example.\") and algorithm is \"hmac-sha256\"; internal tsigState equals Message.TSIG_SIGNED (3); no exception is thrown.",
          "description": "Verify that a TSIG whose key name contains non‑ASCII characters is accepted and correctly stored in the generated TSIGRecord."
        }
      ]
    },
    {
      "group": "setTSIG_largeSecret",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"large.example.\"), \"hmac-sha256\", new byte[5000])"
            }
          ],
          "expected": "Message.isSigned() returns true; Message.getTSIG() is not null and contains a secret of length 5000; tsigState == Message.TSIG_SIGNED (3); Message.numBytes() <= Message.MAXLENGTH (65535) – the library should truncate or handle the large secret without throwing.",
          "description": "Stress‑test the handling of a TSIG with an unusually large secret to ensure the method does not overflow buffers or throw unexpected exceptions."
        }
      ]
    },
    {
      "group": "setTSIG_nullAlgorithm_handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"nullalg.example.\"), null, \"secret\".getBytes())"
            }
          ],
          "expected": "Message.isSigned() returns false; Message.getTSIG() returns null; tsigState == Message.TSIG_FAILED (4); no checked exception propagates from setTSIG.",
          "description": "A TSIG with a null algorithm is invalid; the method should fail gracefully, marking the message as TSIG_FAILED without throwing a checked exception."
        }
      ]
    },
    {
      "group": "setTSIG_preservesHeader",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"preserve.example.\"), \"hmac-md5\", \"secret\".getBytes())"
            }
          ],
          "expected": "After setTSIG, Message.getHeader().getID() is unchanged from its value before the call; Message.isSigned() == true; tsigState == Message.TSIG_SIGNED (3).",
          "description": "Ensure that adding a TSIG does not modify unrelated header fields such as the message ID."
        }
      ]
    },
    {
      "group": "setTSIG_replacesExistingTSIGRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"first.example.\"), \"hmac-md5\", \"first\".getBytes())"
            }
          ],
          "expected": "After first call, Message.isSigned() == true and Message.getTSIG().getName() == \"first.example.\"; after a second call with a different key (see second case), the original TSIGRecord is removed from the additional section and replaced by the new one; tsigState remains Message.TSIG_SIGNED (3).",
          "description": "First call establishes a TSIG; a subsequent call with a new TSIG should replace the existing TSIGRecord rather than appending another."
        },
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"second.example.\"), \"hmac-sha256\", \"second\".getBytes())"
            }
          ],
          "expected": "Message.isSigned() == true; Message.getTSIG().getName() == \"second.example.\"; the additional section contains exactly one TSIGRecord whose key matches the second TSIG; tsigState == Message.TSIG_SIGNED (3).",
          "description": "Second call replaces the first TSIG; verify that only one TSIGRecord exists after replacement."
        }
      ]
    },
    {
      "group": "setTSIG_noSideEffectOnSections",
      "cases": [
        {
          "input": [
            {
              "parameter": "key",
              "value": "new TSIG(Name.fromString(\"noside.example.\"), \"hmac-md5\", \"secret\".getBytes())"
            }
          ],
          "expected": "All non‑TSIG sections (ANSWER, AUTHORITY, ADDITIONAL except the generated TSIG) retain the same Record objects and order as before the call; only the TSIGRecord is added/updated in the ADDITIONAL section; Message.getSection(Message.ADDITIONAL).size() increases by 1 if no TSIG was present before, otherwise stays the same.",
          "description": "Confirm that setTSIG does not inadvertently modify or reorder records in sections other than the TSIG record itself."
        }
      ]
    }
  ]
}