{
  "testCases": [
    {
      "group": "Fudge boundary – timestamp exactly at upper limit",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageTimestampNowPlus300" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Message m contains a TSIGRecord with error code NOERROR (timestamp accepted at +300 s)",
          "description": "The message timestamp is exactly the current time plus TSIG.FUDGE (300 s). The apply method must treat the timestamp as valid and generate a TSIG RR with NOERROR."
        }
      ]
    },
    {
      "group": "Fudge boundary – timestamp exactly at lower limit",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageTimestampNowMinus300" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Message m contains a TSIGRecord with error code NOERROR (timestamp accepted at –300 s)",
          "description": "The message timestamp is exactly the current time minus TSIG.FUDGE (300 s). The apply method must accept the timestamp and generate a TSIG RR with NOERROR."
        }
      ]
    },
    {
      "group": "Fudge exceeded by one second – future timestamp",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageTimestampNowPlus301" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Message m contains a TSIGRecord with error code Rcode.BADTIME",
          "description": "The message timestamp is 301 s in the future, i.e., just beyond the allowed fudge window. The apply method must set the error field to BADTIME."
        }
      ]
    },
    {
      "group": "Fudge exceeded by one second – past timestamp",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageTimestampNowMinus301" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Message m contains a TSIGRecord with error code Rcode.BADTIME",
          "description": "The message timestamp is 301 s in the past, just outside the allowed fudge window. The apply method must set the error field to BADTIME."
        }
      ]
    },
    {
      "group": "Message already contains a TSIG – replacement",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "messageWithExistingTSIG" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA256" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Message m contains exactly one TSIGRecord, the newly generated one (old TSIG replaced)",
          "description": "When the message already has a TSIG RR, apply must remove the existing record and attach a freshly generated TSIG RR based on the supplied old record."
        }
      ]
    },
    {
      "group": "FullSignature false – MAC truncation",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1WithFullMac" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Generated TSIGRecord MAC length equals 10 bytes (default truncated size)",
          "description": "The old TSIGRecord supplies a full‑length SHA‑1 MAC (20 bytes). With fullSignature set to false the apply method must truncate the MAC to the default 10‑byte length."
        }
      ]
    },
    {
      "group": "FullSignature true – full MAC retained",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA1WithFullMac" },
            { "parameter": "boolean fullSignature", "value": true }
          ],
          "expected": "Generated TSIGRecord MAC length equals 20 bytes (full SHA‑1 MAC)",
          "description": "When fullSignature is true the apply method must keep the complete MAC (no truncation). The resulting TSIG RR should contain the full 20‑byte SHA‑1 MAC."
        }
      ]
    },
    {
      "group": "Empty secret key – illegal argument",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "TSIGRecord old", "value": "tsigRecordWithEmptySecretKey" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "IllegalArgumentException with message containing 'secret key length'",
          "description": "If the TSIGRecord references a secret key of zero length, the apply method cannot compute a MAC and must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Algorithm HMAC‑SHA256 handling – default truncation",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA256" },
            { "parameter": "boolean fullSignature", "value": false }
          ],
          "expected": "Generated TSIGRecord MAC length equals 10 bytes (default truncated size) and algorithm name HMAC‑SHA256",
          "description": "The old TSIGRecord uses the HMAC‑SHA256 algorithm. With fullSignature false the MAC must be truncated to the default 10‑byte size while preserving the algorithm identifier."
        }
      ]
    },
    {
      "group": "Algorithm HMAC‑SHA256 handling – full signature",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "TSIGRecord old", "value": "validOldTSIGRecordSHA256" },
            { "parameter": "boolean fullSignature", "value": true }
          ],
          "expected": "Generated TSIGRecord MAC length equals 32 bytes (full SHA‑256 MAC) and algorithm name HMAC‑SHA256",
          "description": "When fullSignature is true and the algorithm is HMAC‑SHA256, the apply method must retain the full 32‑byte MAC."
        }
      ]
    }
  ]
}