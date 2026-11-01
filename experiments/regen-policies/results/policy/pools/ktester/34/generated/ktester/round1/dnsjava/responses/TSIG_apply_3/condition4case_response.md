{
  "testCases": [
    {
      "group": "Null argument handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "null"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "The method should reject a null Message and throw NPE, regardless of error code or old TSIG."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message)"
            },
            {
              "parameter": "int error",
              "value": "-1"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A negative error code is invalid; the method should throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Error code branching",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with no TSIG"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Message contains a new TSIGRecord with RCODE=0 (NOERROR)",
          "description": "When error == 0 and no previous TSIG, a normal TSIG should be generated and attached to the message."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with existing TSIG"
            },
            {
              "parameter": "int error",
              "value": "9"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "mock(TSIGRecord) representing previous TSIG"
            }
          ],
          "expected": "Message contains a new TSIGRecord whose RCODE matches the supplied error (9 = NOTIMP)",
          "description": "When a non‑zero error code is supplied, the generated TSIG must carry that error in its RCODE field."
        }
      ]
    },
    {
      "group": "Old TSIG record handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with no existing TSIG"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Generated TSIGRecord has a zero time‑signed field (no prior record to copy).",
          "description": "When old == null the algorithm should treat this as the first TSIG in the transaction."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with placeholder TSIG"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "mock(TSIGRecord) with known timeSigned value"
            }
          ],
          "expected": "Generated TSIGRecord copies the timeSigned from the old record (or increments it according to spec).",
          "description": "When an old TSIG is supplied, the new TSIG must incorporate the previous time‑signed value to allow proper verification."
        }
      ]
    },
    {
      "group": "Full‑signature flag default (delegation test)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message)"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Same result as calling apply(m, error, old, true)",
          "description": "The three‑argument overload should delegate to the four‑argument version with fullSignature set to true."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) where apply(Message,int,TSIGRecord,true) throws TSIGException"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "TSIGException propagated",
          "description": "If the underlying four‑argument method throws, the three‑argument wrapper must not swallow the exception."
        }
      ]
    }
  ]
}