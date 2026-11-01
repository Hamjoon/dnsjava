{
  "testCases": [
    {
      "group": "apply(Message, TSIGRecord) - Null Message",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "null"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "any non‑null TSIGRecord"
            }
          ],
          "expected": "NullPointerException",
          "description": "The method should reject a null Message and throw a NullPointerException before any processing."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Null Old Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with no TSIG RR"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Message m now contains a TSIG RR generated with default algorithm (HMAC‑SHA256) and Rcode.NOERROR; no exception is thrown.",
          "description": "When the old TSIGRecord is null the method should create a fresh TSIGRecord, sign the message with the default algorithm and error code NOERROR, and attach it to the message."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Valid Old Record (full signature)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message containing a query section and no TSIG"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord previously generated for the same key, algorithm HMAC‑SHA256, with a valid MAC"
            }
          ],
          "expected": "Message m now contains a TSIG RR whose MAC is correctly recomputed using the old record’s key and algorithm; the error code remains NOERROR.",
          "description": "The overload should forward to apply(m, Rcode.NOERROR, old, true). With a non‑null old record the method must verify the old MAC, compute a new MAC over the message plus the old MAC, and replace the TSIG RR."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Old Record with Different Algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with a single question section"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord whose algorithm name is unknown (e.g., \"unknown-alg.\")"
            }
          ],
          "expected": "IllegalArgumentException (or a subclass of GeneralSecurityException) indicating unsupported algorithm",
          "description": "If the old TSIGRecord specifies an algorithm that cannot be mapped to a known Name, the method should abort and throw an exception."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Message Already Contains TSIG",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message that already has a TSIG RR attached"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "The existing TSIG RR is replaced by a newly generated one (full signature) without throwing; the message ends with exactly one TSIG RR.",
          "description": "When a message already carries a TSIG, the method should remove the old one and add a fresh TSIG generated from the supplied old record (null in this case)."
        }
      ]
    },
    {
      "group": "apply(Message, TSIGRecord) - Edge Case: Empty Message Sections",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Message with no question, answer, authority or additional sections"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Message m now contains a valid TSIG RR; the MAC is computed over an empty DNS payload plus the standard TSIG pseudo‑header.",
          "description": "The method must correctly handle an empty DNS message and still generate a valid TSIG record."
        }
      ]
    }
  ]
}