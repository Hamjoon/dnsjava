{
  "tests": [
    {
      "group": "OPT + TSIG combination",
      "cases": [
        {
          "input": [],
          "expected": "String contains the header line from header.toStringWithRcode(...), followed by a blank line, the OPT pseudo‑section text, a newline, and then a line \";; TSIG invalid\" followed by a newline.",
          "description": "Message has a non‑null OPT record, isSigned() returns true, and isVerified() returns false. Verifies that the method prints the OPT pseudo‑section and the TSIG line with the word \"invalid\"."
        },
        {
          "input": [],
          "expected": "String contains the header line from header.toString() (no OPT block) and a line \";; TSIG ok\" followed by a newline, with no \"OPT\" pseudo‑section present.",
          "description": "Message has no OPT record, isSigned() returns true, and isVerified() returns true. Checks that the TSIG ok line is emitted while the OPT block is omitted."
        }
      ]
    },
    {
      "group": "Header null safety",
      "cases": [
        {
          "input": [],
          "expected": "Throws NullPointerException",
          "description": "Message.header is explicitly set to null before calling toString(). The method accesses header without a null‑check, so a NullPointerException is expected."
        }
      ]
    },
    {
      "group": "Section content rendering",
      "cases": [
        {
          "input": [],
          "expected": "After the line \";; Answer:\" the string representation of the added Record appears on the next line.",
          "description": "A simple A record is added to the ANSWER section (section index 1). The test verifies that sectionToString correctly appends the record's toString() output after the section heading."
        }
      ]
    },
    {
      "group": "Custom Rcode handling",
      "cases": [
        {
          "input": [],
          "expected": "String starts with the exact text returned by Header.toStringWithRcode(customRcode) (e.g., \"HEADER RCODE=2\"), followed by a blank line and the rest of the message.",
          "description": "Header is mocked so that getRcode() returns a custom value (2) and toStringWithRcode(2) returns a known string. Ensures that toString() uses the Header.toStringWithRcode path when an OPT record is present."
        }
      ]
    },
    {
      "group": "Opcode UPDATE with TSIG invalid and no OPT",
      "cases": [
        {
          "input": [],
          "expected": "Each of the four section headings starts with \";; \" + Section.updString(i) + \":\" and the message contains a line \";; TSIG invalid\" followed by a newline. No OPT pseudo‑section appears.",
          "description": "Header opcode is set to UPDATE, the message is signed but not verified, and there is no OPT record. Confirms that the UPDATE‑specific section headings are used and that the TSIG line reports \"invalid\"."
        }
      ]
    }
  ]
}