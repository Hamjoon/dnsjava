{
  "tests": [
    {
      "group": "Signed message – verified TSIG with UPDATE opcode",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message instance where header.getOpcode() == Opcode.UPDATE, no OPTRecord, isSigned() returns true, isVerified() returns true"
            }
          ],
          "expected": "String contains a line \";; TSIG ok\" after the header line, then four section headers that use Section.updString(i) (e.g., \";; \" + Section.updString(0) + \":\"), and finally a line \";; Message size: <numBytes()> bytes\". No OPT pseudo‑section text should appear.",
          "description": "Verifies that when the message is signed and the TSIG is verified, the TSIG line is emitted before the UPDATE‑specific section titles."
        }
      ]
    },
    {
      "group": "Signed message – invalid TSIG with UPDATE opcode",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message instance where header.getOpcode() == Opcode.UPDATE, no OPTRecord, isSigned() returns true, isVerified() returns false"
            }
          ],
          "expected": "String contains a line \";; TSIG invalid\" after the header line, then four section headers that use Section.updString(i), and ends with \";; Message size: <numBytes()> bytes\". No OPT pseudo‑section text should appear.",
          "description": "Ensures that the TSIG invalid branch is taken correctly when the message is signed but verification fails, even when the opcode is UPDATE."
        }
      ]
    },
    {
      "group": "Message with OPT and signed verified (non‑UPDATE opcode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message instance where header.getOpcode() != Opcode.UPDATE, OPTRecord present, isSigned() returns true, isVerified() returns true"
            }
          ],
          "expected": "String starts with header.toStringWithRcode(rcode) + \"\\n\\n\", followed by the OPT pseudo‑section text (as produced by OPTRecord.printPseudoSection), then a newline, then a line \";; TSIG ok\", then four section headers that use Section.longString(i), and finally the size line.",
          "description": "Tests the combination of an OPT pseudo‑section and a verified TSIG when the opcode is not UPDATE, checking ordering of header, OPT, TSIG, and section titles."
        }
      ]
    },
    {
      "group": "Message with OPT and signed invalid (non‑UPDATE opcode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message instance where header.getOpcode() != Opcode.UPDATE, OPTRecord present, isSigned() returns true, isVerified() returns false"
            }
          ],
          "expected": "String starts with header.toStringWithRcode(rcode) + \"\\n\\n\", then the OPT pseudo‑section, then a newline, then a line \";; TSIG invalid\", then four section headers using Section.longString(i), and ends with the size line.",
          "description": "Ensures that when a message contains an OPT pseudo‑section and an unsigned/invalid TSIG, the TSIG invalid line appears after the OPT block and before the normal section titles."
        }
      ]
    },
    {
      "group": "Message with OPT and signed invalid with UPDATE opcode",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message instance where header.getOpcode() == Opcode.UPDATE, OPTRecord present, isSigned() returns true, isVerified() returns false"
            }
          ],
          "expected": "String starts with header.toStringWithRcode(rcode) + \"\\n\\n\", then the OPT pseudo‑section, then a newline, then a line \";; TSIG invalid\", then four section headers that use Section.updString(i), and finally \";; Message size: <numBytes()> bytes\".",
          "description": "Covers the case where an UPDATE‑type message has both an OPT pseudo‑section and an invalid TSIG, verifying that the UPDATE‑specific section titles are used after the TSIG line."
        }
      ]
    }
  ]
}