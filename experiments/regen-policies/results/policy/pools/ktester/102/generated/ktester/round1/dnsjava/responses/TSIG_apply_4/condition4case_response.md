Invalid control character at: line 10 column 38 (char 198)
{
  "tests": [
    {
      "group": "Standard successful signing",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "new Message() // empty DNS message"
            },
            {
              "parameter": "error",
              "value": "0 // Rcode.NOERROR"
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
          "expected": "Message contains a TSIGRecord in Section.ADDITIONAL and m.tsigState == Message.TSIG_SIGNED",
          "description": "Apply TSIG with no previous record, no error, and a short (non‑full) signature. The method should generate a TSIGRecord, add it to the ADDITIONAL section and mark the message as signed."
        },
        {
          "input": [
            {
              "parameter": "m",
              "value": "new Message() // empty DNS message"
            },
            {
              "parameter": "error",
              "value": "0"
            },
            {
              "parameter": "old",
              "value": "new TSIGRecord(Name.fromString(\"example.com.\"), 0, 0, new byte[0])"
            },
            {
              "parameter": "fullSignature",
              "value": "true"
            }
          ],
          "expected": "Message contains a TSIGRecord in Section.ADDITIONAL and m.tsigState == Message.TSIG_SIGNED",
          "description": "Apply TSIG when a previous TSIGRecord is supplied and a full signature is requested. The method must still add the new record and set the signed flag."
        }
      ]
    },
    {
      "group": "Error handling (non‑zero rcode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "new Message()"
            },
            {
              "parameter": "error",
              "value": "9 // Rcode.BADKEY"
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
          "expected": "Message contains a TSIGRecord with error code 9 and m.tsigState == Message.TSIG_SIGNED",
          "description": "When an error code other than NOERROR is supplied, generate should embed that error in the TSIGRecord. The apply method must still add the record and mark the message as signed."
        }
      ]
    },
    {
      "group": "Null Message argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "null"
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
          "expected": "NullPointerException is thrown",
          "description": "Calling apply with a null Message should result in an immediate NPE because the method dereferences the message to obtain its wire format."
        }
      ]
    },
    {
      "group": "Message already contains additional records",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message msg = new Message(); msg.addRecord(new ARecord(Name.fromString(\"a.example.\"), DClass.IN, 300, InetAddress.getByName(\"1.2.3.4\")), Section.ADDITIONAL); msg"
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
          "expected": "Message now contains the original additional record plus a new TSIGRecord; m.tsigState == Message.TSIG_SIGNED",
          "description": "Verify that apply does not replace existing additional records but appends the generated TSIGRecord."
        }
      ]
    },
    {
      "group": "Custom generate stub (verify interaction)",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "new Message()"
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
              "value": "true"
            }
          ],
          "expected": "A stubbed TSIG subclass that overrides generate to return a known Record X; after apply, Message contains exactly Record X in ADDITIONAL and tsigState == Message.TSIG_SIGNED",
          "description": "Using a test‑specific subclass of TSIG to control the output of generate ensures the apply method forwards the returned Record unchanged."
        }
      ]
    }
  ]
}