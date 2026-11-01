{
  "testCases": [
    {
      "group": "Message without additional section",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message containing a question and answer section but no additional records"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "valid TSIGRecord with algorithm HMAC_SHA256 and key \"testkey\""
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "A TSIGRecord is appended to the message’s additional section; the MAC is calculated over the DNS header and the question section only (per RFC 2845). No existing additional records are disturbed.",
          "description": "Ensures that `apply` correctly adds a TSIG record when the message has no prior additional records and that the MAC covers only the required parts when `fullSignature` is false."
        }
      ]
    },
    {
      "group": "Old TSIGRecord with different key name",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that already contains a TSIGRecord with key name \"old.example.\""
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with key name \"new.example.\", algorithm HMAC_SHA1, same secret as the existing TSIG"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "The existing TSIGRecord is replaced by a new one whose key name matches the supplied `old` record (\"new.example.\") and whose MAC is recomputed using the new key name. All other fields (algorithm, error code) are preserved from the supplied `old` record.",
          "description": "Verifies that when an old TSIGRecord with a different key name is supplied, `apply` discards the previously attached TSIG and signs the message with the new key name."
        }
      ]
    },
    {
      "group": "Algorithm HMAC_MD5 support (legacy)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a single question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord built with algorithm name \"HMAC-MD5.SIG-ALG.REG.INT.\" and secret \"md5key\""
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "A TSIGRecord is added; its MAC is the MD5‑based HMAC of the header and question section. No exception is thrown, confirming legacy algorithm handling.",
          "description": "Checks that the deprecated HMAC‑MD5 algorithm is still recognised and that a correct MAC is produced."
        }
      ]
    },
    {
      "group": "Zero‑length secret key",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message containing a question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm HMAC_SHA256 and an empty (zero‑length) secret key"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "TSIGRecord is added; the MAC consists of the HMAC‑SHA256 of the message using an empty key (which is a defined value, not an error). No exception is thrown.",
          "description": "Ensures that the method can handle a secret key of length zero without crashing, as allowed by the underlying JCE implementation."
        }
      ]
    },
    {
      "group": "Empty Message (no sections) with fullSignature true",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message created with the default constructor and no sections added"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "valid TSIGRecord with algorithm HMAC_SHA384 and secret \"emptymsg\""
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "A TSIGRecord is appended; the MAC is computed over the DNS header only (since there are no question/answer/additional sections). The method does not throw.",
          "description": "Validates that `apply` works for a completely empty DNS message and that the full‑signature flag does not cause an out‑of‑bounds error."
        }
      ]
    },
    {
      "group": "Message with only answer section and fullSignature false",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that contains an answer section but no question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "valid TSIGRecord with algorithm HMAC_SHA512 and secret \"ansonly\""
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "TSIGRecord is added; the MAC is calculated over the DNS header and the (non‑existent) question section only, i.e., the header alone. The answer section is not part of the MAC.",
          "description": "Confirms that when `fullSignature` is false the implementation follows RFC 2845 even if the message lacks a question section, ignoring the answer section."
        }
      ]
    },
    {
      "group": "Timestamp precision handling (seconds only)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a question section; system clock set to a fixed Instant with nanosecond part 123456789"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "valid TSIGRecord with algorithm HMAC_SHA1 and secret \"precise\""
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "The generated TSIGRecord’s time‑signed field contains a value truncated to whole seconds (nanoseconds are zero). No exception is thrown.",
          "description": "Ensures that `apply` normalises the timestamp to second granularity, as required by the TSIG specification."
        }
      ]
    },
    {
      "group": "Old TSIGRecord with mismatched algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord whose algorithm name is HMAC_SHA256 but the secret key is intended for HMAC_SHA1 (different key length)"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "false"
            }
          ],
          "expected": "IllegalArgumentException (or GeneralSecurityException wrapped) thrown because the algorithm name does not match the secret key’s expected size/format.",
          "description": "Checks that the method validates that the supplied algorithm is compatible with the secret key and fails fast on a mismatch."
        }
      ]
    },
    {
      "group": "Existing TSIGRecord present while old parameter is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that already contains a TSIGRecord with key \"existing.example.\""
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            },
            {
              "parameter": "boolean fullSignature",
              "value": "true"
            }
          ],
          "expected": "The existing TSIGRecord is replaced by a newly generated one (key name derived from the message’s TSIGRecord if possible, otherwise a default). No exception is thrown.",
          "description": "Verifies that when `old` is null but the message already carries a TSIG, the method treats the attached record as the basis for re‑signing rather than throwing an error."
        }
      ]
    }
  ]
}