{
  "test_cases": [
    {
      "group": "Null secret key in TSIGRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordWithNullSecretKey"
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "IllegalArgumentException with message containing 'secret key is null'",
          "description": "The old TSIGRecord references a null secret key. The apply method must validate the key before MAC computation and throw an IllegalArgumentException."
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
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordWithNullAlgorithm"
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "IllegalArgumentException with message containing 'algorithm name is null'",
          "description": "The supplied TSIGRecord has a null algorithm identifier. The method must reject such a record and raise an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid secret key length for HMAC‑SHA256",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordSHA256WithShortKey"
            },
            {
              "parameter": "boolean fullSignature",
              "value": true
            }
          ],
          "expected": "GeneralSecurityException",
          "description": "The old TSIGRecord specifies HMAC‑SHA256 but provides a secret key that is too short for the algorithm. When the MAC instance is initialized, the JCE provider throws a GeneralSecurityException, which should propagate from apply."
        }
      ]
    },
    {
      "group": "Unsupported MAC algorithm causing JCE failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordWithSupportedAlgButJceUnavailable"
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "GeneralSecurityException",
          "description": "The TSIGRecord uses a known algorithm name (e.g., HMAC‑SHA384) but the underlying JCE provider cannot supply a Mac instance for it, resulting in a GeneralSecurityException that must be thrown by apply."
        }
      ]
    },
    {
      "group": "Immutable Message – cannot attach TSIG",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "immutableMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "validOldTSIGRecord"
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "IllegalStateException with message containing 'Message is immutable'",
          "description": "When the Message instance is marked as immutable (e.g., after being sent), apply must refuse to modify it and throw an IllegalStateException."
        }
      ]
    }
  ]
}