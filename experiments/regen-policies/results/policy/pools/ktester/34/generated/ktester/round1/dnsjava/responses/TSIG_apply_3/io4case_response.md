{
  "testCases": [
    {
      "group": "Time fudge boundary handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with no TSIG, current time = Instant.now()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with timeSigned = Instant.now().minus(TSIG.FUDGE).plusSeconds(1) (just inside the fudge window)"
            }
          ],
          "expected": "Message receives a TSIGRecord whose timeSigned is within the allowed FUDGE duration; no exception is thrown.",
          "description": "When the previous TSIG’s timeSigned is just inside the allowed fudge window, apply should accept it and generate a new TSIG without raising a BADTIME error."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with no TSIG, current time = Instant.now()"
            },
            {
              "parameter": "int error",
              "value": "0"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "TSIGRecord with timeSigned = Instant.now().minus(TSIG.FUDGE).minusSeconds(1) (just outside the fudge window)"
            }
          ],
          "expected": "TSIGException (or IllegalArgumentException) indicating BADTIME because the old timeSigned is outside the allowed fudge period.",
          "description": "When the previous TSIG’s timeSigned is just outside the allowed fudge window, apply must reject it and surface a BADTIME‑related error."
        }
      ]
    },
    {
      "group": "Algorithm mismatch between old TSIG and new",
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
              "value": "TSIGRecord using algorithm HMAC_SHA256"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the algorithm of the supplied old TSIG differs from the algorithm configured in the TSIG instance (default HMAC_MD5), the method should reject the call with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Empty secret key handling",
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
          "expected": "TSIGException (or IllegalArgumentException) because the TSIG instance was constructed with an empty secret key.",
          "description": "A TSIG object whose secret key byte array is zero‑length cannot compute a MAC; apply must fail with a clear exception."
        }
      ]
    },
    {
      "group": "Repeated apply on same Message",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) that already contains a TSIGRecord"
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
          "expected": "IllegalStateException (or TSIGException) indicating that a TSIG is already present on the message.",
          "description": "Calling apply on a Message that already carries a TSIG should not silently replace it; the method is expected to reject the operation."
        }
      ]
    },
    {
      "group": "Bad time error handling (RCODE = BADTIME)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with no TSIG"
            },
            {
              "parameter": "int error",
              "value": "18"   ,
              "note": "18 is the DNS RCODE for BADTIME"
            },
            {
              "parameter": "TSIGRecord old",
              "value": "null"
            }
          ],
          "expected": "Message contains a TSIGRecord whose RCODE field equals 18 (BADTIME) and whose timeSigned is set to the current Instant.",
          "description": "When the caller supplies the BADTIME error code, apply must embed that code in the generated TSIG while also setting a fresh timeSigned value."
        }
      ]
    },
    {
      "group": "Full‑signature length verification (default true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "mock(Message) with a few question and answer records"
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
          "expected": "Generated TSIGRecord MAC length matches the expected output size of the default algorithm (16 bytes for HMAC‑MD5).",
          "description": "The three‑argument overload forwards to the four‑argument version with fullSignature=true; consequently the full message (including all sections) is covered by the MAC. The test verifies the MAC size matches the algorithm’s output length."
        }
      ]
    }
  ]
}