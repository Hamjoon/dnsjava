{
  "testCases": [
    {
      "group": "Valid inputs – normal signature",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
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
          "expected": "Message m contains a TSIGRecord with a standard (truncated) MAC and error code NOERROR",
          "description": "Apply TSIG with a valid message and an existing TSIG record using the default (non‑full) signature. The method should generate a new TSIG RR, attach it to the message and set the MAC length to the default size."
        }
      ]
    },
    {
      "group": "Valid inputs – full signature",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "validOldTSIGRecord"
            },
            {
              "parameter": "boolean fullSignature",
              "value": true
            }
          ],
          "expected": "Message m contains a TSIGRecord with a full‑length MAC (no truncation) and error code NOERROR",
          "description": "Apply TSIG with the fullSignature flag set to true. The method should generate a TSIG RR that contains the complete MAC value rather than a truncated one."
        }
      ]
    },
    {
      "group": "Old TSIGRecord is null – generate new record",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": null
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "Message m contains a newly generated TSIGRecord (no old record to chain) and error code NOERROR",
          "description": "When the old TSIGRecord argument is null the method must generate a fresh TSIG RR and attach it to the message."
        }
      ]
    },
    {
      "group": "Null Message argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": null
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
          "expected": "NullPointerException",
          "description": "Passing a null Message should cause the method to throw a NullPointerException because it cannot add a TSIG RR to a non‑existent message."
        }
      ]
    },
    {
      "group": "Unsupported algorithm in old TSIGRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordWithUnknownAlgorithm"
            },
            {
              "parameter": "boolean fullSignature",
              "value": false
            }
          ],
          "expected": "IllegalArgumentException with message containing 'unsupported algorithm'",
          "description": "If the old TSIGRecord contains an algorithm name that the TSIG class does not recognise, the method should reject the request and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Non‑zero error code handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
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
          "expected": "Message m contains a TSIGRecord whose error field equals Rcode.FORMERR (or other non‑zero error code)",
          "description": "When the internal call to apply(Message, int, TSIGRecord, boolean) is invoked with an error code other than NOERROR, the resulting TSIG RR must carry that error code."
        }
      ]
    },
    {
      "group": "Full signature requested but old record lacks MAC",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "validMessage"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "tsigRecordWithoutMac"
            },
            {
              "parameter": "boolean fullSignature",
              "value": true
            }
          ],
          "expected": "IllegalStateException with message containing 'MAC missing'",
          "description": "If fullSignature is true but the supplied old TSIGRecord does not contain a MAC (e.g., it is a placeholder), the method cannot compute a chain and must throw an IllegalStateException."
        }
      ]
    },
    {
      "group": "Fudge time window exceeded",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "messageWithTimestampOutsideFudge"
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
          "expected": "TSIGRecord attached to Message with error code Rcode.BADTIME",
          "description": "If the timestamp in the message is older/newer than the current time by more than TSIG.FUDGE (300 s), the generated TSIG RR must indicate a BADTIME error."
        }
      ]
    }
  ]
}