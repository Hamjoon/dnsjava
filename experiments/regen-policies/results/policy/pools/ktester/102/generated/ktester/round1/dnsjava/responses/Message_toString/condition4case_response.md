{
  "testCases": [
    {
      "group": "OPT handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), no OPT record, not signed"
            }
          ],
          "expected": "Header.toString() output followed by a newline, then for each of the four sections the line \";; <SectionName>:\" where <SectionName> is from Section.longString(i), no TSIG line, and the final line \";; Message size: <size> bytes\"",
          "description": "Verifies the branch where opt == null, the message is not signed, and the opcode is not UPDATE. The toString should use the simple header string and longString section titles."
        },
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), OPT record present, not signed"
            }
          ],
          "expected": "Header.toStringWithRcode(rcode) output, a blank line, the OPT pseudo‑section printed by OPTRecord.printPseudoSection, a blank line, then the four sections with longString titles, no TSIG line, and the final size line",
          "description": "Covers the opt != null branch. The method must call Header.toStringWithRcode and include the OPT pseudo‑section before the normal sections."
        }
      ]
    },
    {
      "group": "TSIG handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), signed = true, verified = true, no OPT"
            }
          ],
          "expected": "Contains the line \";; TSIG ok\" after the header (or OPT) block",
          "description": "Tests the signed && verified path. The TSIG line should contain \"ok\"."
        },
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), signed = true, verified = false, no OPT"
            }
          ],
          "expected": "Contains the line \";; TSIG invalid\" after the header (or OPT) block",
          "description": "Tests the signed && !verified path. The TSIG line should contain \"invalid\"."
        }
      ]
    },
    {
      "group": "Opcode handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=UPDATE, rcode=NOERROR), not signed, no OPT"
            }
          ],
          "expected": "Each of the four section headers is produced by Section.updString(i) (e.g., \";; Update Section 0:\") instead of Section.longString(i)",
          "description": "Ensures the branch where header.getOpcode() == Opcode.UPDATE uses the UPDATE‑specific section titles."
        },
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), not signed, no OPT"
            }
          ],
          "expected": "Each of the four section headers is produced by Section.longString(i)",
          "description": "Ensures the normal (non‑UPDATE) path uses longString for section titles."
        }
      ]
    }
  ]
}