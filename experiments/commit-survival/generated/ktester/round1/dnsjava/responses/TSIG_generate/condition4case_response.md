{
  "testCases": [
    {
      "group": "Null checks",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "null" },
            { "parameter": "byte[] b", "value": "new byte[]{1,2,3}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "The method should reject a null Message and throw NPE before any processing."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "null" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "The method should reject a null key byte array and throw NPE."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "new byte[]{1,2,3}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord instance",
          "description": "All non‑null arguments should result in a TSIGRecord being generated."
        }
      ]
    },
    {
      "group": "Error handling (error != 0)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "9" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error code 9",
          "description": "When a non‑zero error code is supplied the generated TSIGRecord must contain that error code."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "9" },
            { "parameter": "TSIGRecord old", "value": "validOldRecord" }
          ],
          "expected": "TSIGRecord with error code 9 and proper continuation data",
          "description": "Non‑zero error with an existing old TSIG should still embed the error while preserving the old record’s MAC for continuation."
        }
      ]
    },
    {
      "group": "Old TSIG presence (old == null vs not null)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord without previous MAC field",
          "description": "When old is null the generated TSIGRecord must not contain a previous MAC (the field should be empty)."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "validOldRecord" }
          ],
          "expected": "TSIGRecord whose previous MAC matches old.getSignature()",
          "description": "When old is supplied the new TSIGRecord must embed the old MAC to allow proper chaining of TSIG signatures."
        }
      ]
    },
    {
      "group": "Full signature flag (always true in this overload)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord containing the full HMAC (not truncated)",
          "description": "The overload without the boolean forces fullSignature=true, so the resulting MAC must be the complete HMAC output."
        }
      ]
    },
    {
      "group": "Algorithm handling (different key algorithms)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageSHA256" },
            { "parameter": "byte[] b", "value": "validKeyBytesSHA256" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with algorithm name HMAC_SHA256",
          "description": "When the Message contains an algorithm of SHA‑256 the generated TSIGRecord must use the HMAC‑SHA256 algorithm name."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageMD5" },
            { "parameter": "byte[] b", "value": "validKeyBytesMD5" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with algorithm name HMAC_MD5",
          "description": "When the Message specifies MD5 the generated TSIGRecord must use the HMAC‑MD5 algorithm name."
        }
      ]
    },
    {
      "group": "Exception handling (unsupported algorithm)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageUnsupportedAlg" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "GeneralSecurityException",
          "description": "If the algorithm indicated by the Message cannot be mapped to a known HMAC algorithm, the method should throw a GeneralSecurityException."
        }
      ]
    }
  ]
}