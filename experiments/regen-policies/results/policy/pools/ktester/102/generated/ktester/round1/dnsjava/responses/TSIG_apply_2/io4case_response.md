{
  "testCases": [
    {
      "group": "Old TSIGRecord with non‑default algorithm and fullSignature false",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message.createQuery(Name.fromConstantString(\"example.org.\"), Type.A)"
            },
            {
              "parameter": "old",
              "value": "new TSIGRecord(Name.fromConstantString(\"example.org.\"), TSIG.HMAC_MD5, 0, new byte[]{0x0A,0x0B,0x0C,0x0D}, 0, 0, 0, 0, 0, 0)"
            },
            {
              "parameter": "fullSignature",
              "value": "false"
            }
          ],
          "expected": "A new TSIG RR is added to the Message; the MAC length is truncated according to MD5’s default (10 bytes) because fullSignature is false, even though the old record used HMAC‑MD5.",
          "description": "When an old TSIGRecord that uses a non‑default algorithm (HMAC‑MD5) is supplied and fullSignature is false, apply should generate a TSIG record with a truncated MAC appropriate for that algorithm."
        }
      ]
    },
    {
      "group": "Message already contains a TSIG RR",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message.createQuery(Name.fromConstantString(\"example.net.\"), Type.A).addRecord(new TSIGRecord(Name.fromConstantString(\"example.net.\"), TSIG.HMAC_SHA256, 0, new byte[]{0x01}, 0, 0, 0, 0, 0, 0), Section.ADDITIONAL)"
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
          "expected": "The original TSIG RR is replaced; after apply the Message contains exactly one TSIG RR whose MAC length equals the full SHA‑256 output (32 bytes).",
          "description": "If the Message already has a TSIG record, apply should remove/replace it with a freshly generated one rather than appending a second TSIG RR."
        }
      ]
    },
    {
      "group": "Generated TSIGRecord error field verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message.newResponse(Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A))"
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
          "expected": "The TSIG RR attached to the response has its error field set to Rcode.NOERROR (0), confirming that apply always forwards NOERROR to the four‑argument overload.",
          "description": "Even when the Message is a response (which may later be set to a different Rcode), apply(Message, TSIGRecord, boolean) must pass Rcode.NOERROR to the underlying implementation."
        }
      ]
    },
    {
      "group": "Apply on empty Message (no sections)",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "new Message()"
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
          "expected": "A TSIG RR is still generated and attached; the MAC is computed over an empty DNS message body and therefore has a deterministic length equal to the full algorithm output.",
          "description": "The method must handle a Message that contains no questions, answers, authorities, or additional records without throwing exceptions."
        }
      ]
    },
    {
      "group": "Apply with multiple additional records",
      "cases": [
        {
          "input": [
            {
              "parameter": "m",
              "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A).addRecord(new ARecord(Name.fromConstantString(\"example.com.\"), DClass.IN, 300, InetAddress.getByName(\"192.0.2.1\")), Section.ADDITIONAL).addRecord(new TXTRecord(Name.fromConstantString(\"example.com.\"), DClass.IN, 300, \"extra\"), Section.ADDITIONAL)"
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
          "expected": "The generated TSIG RR reflects the presence of the two additional records; the MAC is truncated (per default policy) but verification of length matches the algorithm’s truncated size (e.g., 10 bytes for SHA‑256).",
          "description": "When the Message contains several additional records, apply must incorporate all of them into the MAC calculation, resulting in a valid (though truncated) TSIG signature."
        }
      ]
    }
  ]
}