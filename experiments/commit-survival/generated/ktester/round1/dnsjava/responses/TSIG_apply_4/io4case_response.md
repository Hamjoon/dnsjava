{
  "testCases": [
    {
      "group": "apply - forwards all parameters correctly (spy verification)",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message spy where toWire() returns byte[3]{0x01,0x02,0x03} and addRecord() is a no‑op"
            },
            {
              "parameter": "error",
              "value": "42"
            },
            {
              "parameter": "old",
              "value": "TSIGRecord mock (previous TSIG)"
            },
            {
              "parameter": "fullSignature",
              "value": "true"
            }
          ],
          "expected": "TSIG.generate(Message, byte[3]{0x01,0x02,0x03}, 42, old, true) is invoked exactly once and the Record returned by generate is passed to Message.addRecord(..., Section.ADDITIONAL); Message.tsigState becomes Message.TSIG_SIGNED",
          "description": "Using a Mockito spy on TSIG we verify that apply() forwards the exact byte array produced by Message.toWire(), the supplied error code, the old TSIGRecord and the fullSignature flag to the private generate(...) overload. The test also checks that the generated Record is added to the ADDITIONAL section and that the message state is set to TSIG_SIGNED."
        }
      ]
    },
    {
      "group": "apply - overwrites existing tsigState",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message mock with tsigState initially set to Message.TSIG_VERIFIED and toWire() returning empty byte[]"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "old",
              "value": "null"
            },
            {
              "parameter": "fullSignature",
              "value": "false"
            }
          ],
          "expected": "After apply() returns, Message.tsigState == Message.TSIG_SIGNED (previous value overwritten)",
          "description": "Ensures that apply() does not preserve a pre‑existing tsigState (e.g., TSIG_VERIFIED) but always sets the state to TSIG_SIGNED after signing."
        }
      ]
    },
    {
      "group": "apply - propagates exception from Message.addRecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message mock where addRecord(...) throws IllegalStateException(\"boom\") and toWire() returns empty byte[]"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "old",
              "value": "null"
            },
            {
              "parameter": "fullSignature",
              "value": "false"
            }
          ],
          "expected": "IllegalStateException with message \"boom\" is thrown by apply()",
          "description": "Verifies that apply() does not swallow exceptions thrown by Message.addRecord; the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "apply - old non‑null, error non‑zero, fullSignature true",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message mock with toWire() returning byte[1]{0x7F} and addRecord() stubbed"
            },
            {
              "parameter": "error",
              "value": "9"
            },
            {
              "parameter": "old",
              "value": "TSIGRecord mock (previous TSIG)"
            },
            {
              "parameter": "fullSignature",
              "value": "true"
            }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate; generate receives the non‑zero error code, the old TSIGRecord and fullSignature=true",
          "description": "Covers the combination where a previous TSIGRecord is present, an error code is supplied, and a full signature is requested. The test confirms that apply() still signs the message, forwards all parameters, and updates the state."
        }
      ]
    },
    {
      "group": "apply - old null, error non‑zero, fullSignature true",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message mock where toWire() returns byte[2]{0xAA,0xBB} and addRecord() stubbed"
            },
            {
              "parameter": "error",
              "value": "5"
            },
            {
              "parameter": "old",
              "value": "null"
            },
            {
              "parameter": "fullSignature",
              "value": "true"
            }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate; generate receives error=5, old=null, fullSignature=true",
          "description": "Ensures that even when no previous TSIGRecord is supplied and a non‑zero DNS error code is present, a full‑signature TSIG is still generated and the message state is set correctly."
        }
      ]
    }
  ]
}