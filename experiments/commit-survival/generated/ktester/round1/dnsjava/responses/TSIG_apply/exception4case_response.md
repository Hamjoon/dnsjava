{
  "test_cases": [
    {
      "group": "apply_oldRecordNullAlgorithm_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a simple question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm = null, key name = Name.fromString(\"example.\") and valid key data"
            }
          ],
          "expected": "IllegalArgumentException is thrown indicating that the algorithm name in the supplied TSIGRecord must not be null.",
          "description": "The apply method must validate that the algorithm field of the old TSIGRecord is non‑null before attempting to map it to a MAC algorithm. Supplying a null algorithm should cause an immediate IllegalArgumentException."
        }
      ]
    },
    {
      "group": "apply_oldRecordNullKeyName_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a single answer record"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm = HMAC_SHA256, key name = null, key data = byte[16] {0x00..0x0F}"
            }
          ],
          "expected": "IllegalArgumentException is thrown because the key name (owner name) of the TSIGRecord is required.",
          "description": "Even when the algorithm and key material are valid, a null key name should be rejected by apply, as the name is part of the TSIG signing data."
        }
      ]
    },
    {
      "group": "apply_oldRecordNullKeyData_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with no existing TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm = HMAC_SHA1, key name = Name.fromString(\"example.\"), key data = null"
            }
          ],
          "expected": "IllegalArgumentException is thrown indicating that the secret key bytes must not be null.",
          "description": "The signing routine requires a non‑null byte array for the secret key. Supplying a TSIGRecord with a null key should result in an IllegalArgumentException before any cryptographic operation is attempted."
        }
      ]
    },
    {
      "group": "apply_oldRecordEmptyKeyData_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance with a question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with algorithm = HMAC_SHA256, key name = Name.fromString(\"example.\"), key data = byte[0] {}"
            }
          ],
          "expected": "IllegalArgumentException is thrown because the secret key length is zero, which is invalid for any HMAC algorithm.",
          "description": "A zero‑length key is technically not null but still unusable; the method should detect this and reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "apply_messageImmutable_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message instance that has been converted to wire format (via toWire()) and then marked as read‑only/immutable"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "Valid TSIGRecord (algorithm HMAC_SHA256, key \"immutablekey\", timeSigned = now, error = Rcode.NOERROR)"
            }
          ],
          "expected": "IllegalStateException is thrown because the Message object is immutable and cannot be modified to add a TSIG record.",
          "description": "apply attempts to insert a new TSIGRecord into the message. If the Message has been sealed (e.g., after serialization), the operation must fail with an IllegalStateException."
        }
      ]
    }
  ]
}