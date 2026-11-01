{
  "test_cases": [
    {
      "group": "OPT and TSIG interaction",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), signed=true, verified=true, OPT record present"
            }
          ],
          "expected": "Output starts with Header.toStringWithRcode(rcode), followed by a blank line, then the OPT pseudo‑section printed by OPTRecord.printPseudoSection, another blank line, then a line \";; TSIG ok\", then for each of the four sections a line \";; <SectionName>:\" where <SectionName> is from Section.longString(i), each section content, a blank line after each, and finally \";; Message size: <size> bytes\" where <size> equals message.numBytes().",
          "description": "Ensures that when both an OPT record and a valid signed TSIG are present, the TSIG line appears after the OPT pseudo‑section and before the section listings."
        },
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), signed=true, verified=false, OPT record present"
            }
          ],
          "expected": "Same ordering as the previous case, but the TSIG line reads \";; TSIG invalid\" instead of \"ok\".",
          "description": "Verifies the signed‑but‑not‑verified branch when an OPT record is also present."
        }
      ]
    },
    {
      "group": "Signed flag false but verified true",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), signed=false, verified=true, no OPT"
            }
          ],
          "expected": "The output contains only the header line (Header.toString()), the four section titles from Section.longString(i), their contents, and the size line. No line beginning with \";; TSIG\" appears.",
          "description": "Confirms that a TSIG line is emitted only when the message is actually signed, even if the internal verified flag is true."
        }
      ]
    },
    {
      "group": "UPDATE opcode with TSIG and OPT",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=UPDATE, rcode=NOERROR), signed=true, verified=true, OPT record present"
            }
          ],
          "expected": "Header.toStringWithRcode(rcode) + blank line + OPT pseudo‑section + blank line + \";; TSIG ok\" line + for each of the four sections a line \";; <UpdateSectionName>:\" where the name comes from Section.updString(i) + section contents + final size line.",
          "description": "Tests the combination of UPDATE opcode, an OPT record, and a verified TSIG, ensuring the correct mix of toStringWithRcode, OPT pseudo‑section, TSIG line, and UPDATE‑specific section titles."
        }
      ]
    },
    {
      "group": "Empty sections output",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), no records in any section, no OPT, not signed"
            }
          ],
          "expected": "Header line followed by a newline, then for i = 0..3 a line \";; <SectionName>:\" (Section.longString(i)), immediately followed by a blank line (because the section is empty), and finally \";; Message size: <size> bytes\" where <size> equals the size of an empty DNS message (typically 12 bytes for the header).",
          "description": "Ensures that when sections contain no records the toString method still prints the section headers and does not throw or omit lines."
        }
      ]
    },
    {
      "group": "Message size correctness",
      "cases": [
        {
          "input": [
            {
              "parameter": "message",
              "value": "Message with header (opcode=QUERY, rcode=NOERROR), one A record in the ANSWER section, no OPT, not signed"
            }
          ],
          "expected": "The final line of the output is \";; Message size: X bytes\" where X is exactly the integer returned by message.numBytes() for this concrete message (header + question + one A record).",
          "description": "Validates that the size reported by toString matches the actual byte length computed by numBytes()."
        }
      ]
    }
  ]
}