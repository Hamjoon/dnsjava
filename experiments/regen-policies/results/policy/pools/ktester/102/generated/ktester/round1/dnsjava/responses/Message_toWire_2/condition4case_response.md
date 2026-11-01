{
  "testCases": [
    {
      "group": "EmptyMessage",
      "cases": [
        {
          "input": [],
          "expected": "byte array of length 12 (DNS header only, all zero fields)",
          "description": "Encode a newly constructed Message with no header modifications, no question, and no records. The toWire() method should produce only the DNS header (12 bytes) and set Message.size accordingly."
        }
      ]
    },
    {
      "group": "MessageWithQuestion",
      "cases": [
        {
          "input": [],
          "expected": "byte array whose length equals 12 (header) + encoded question length; the question section must be present and correctly encoded.",
          "description": "Create a Message using Message.newQuery(record) where `record` is a simple A query for example.com. The toWire() call must encode the header and the single question record, exercising the branch that processes the QUESTION section."
        }
      ]
    },
    {
      "group": "MessageWithAnswerAndAuthority",
      "cases": [
        {
          "input": [],
          "expected": "byte array containing header, question, answer RRset and authority RRset; size field must reflect total bytes.",
          "description": "Add one answer Record (A) and one authority Record (NS) to the Message. The toWire() method must traverse SECTION_ANSWER and SECTION_AUTHORITY, testing the branch that iterates over non‑empty sections."
        },
        {
          "input": [],
          "expected": "byte array where the answer RRset is omitted because its type is not allowed in the ANSWER section (e.g., OPT record).",
          "description": "Insert an OPTRecord into the ANSWER section (illegal). The internal `isTypeAllowedInSection` check should cause the record to be skipped, covering the branch that validates record type per section."
        }
      ]
    },
    {
      "group": "MessageWithOPT",
      "cases": [
        {
          "input": [],
          "expected": "byte array that includes an OPT pseudo‑record in the additional section; the OPT record must be encoded after all other additional records.",
          "description": "Add an OPTRecord via Message.getHeader().setFlag(....) or Message.setOPT(...). This tests the branch that specially handles the OPT pseudo‑record when writing the ADDITIONAL section."
        }
      ]
    },
    {
      "group": "MessageWithTSIGSigned",
      "cases": [
        {
          "input": [],
          "expected": "byte array ending with a TSIG RR (type 250) and the TSIG MAC; Message.tsigState must be TSIG_SIGNED after toWire().",
          "description": "Create a Message, set a TSIG key with Message.setTSIG(key) and then call Message.sign() (or the equivalent API). The toWire() method will reach the branch that appends a signed TSIG record, testing TSIG state handling."
        },
        {
          "input": [],
          "expected": "byte array where the TSIG is present but its MAC is empty because the message was not signed (TSIG_UNSIGNED).",
          "description": "Add a TSIG key but do not sign the message; the toWire() method should still write the TSIG placeholder, covering the branch for TSIG_UNSIGNED."
        }
      ]
    },
    {
      "group": "MessageExceedingMaxLength",
      "cases": [
        {
          "input": [],
          "expected": "MessageSizeExceededException thrown from toWire(int maxLength, boolean truncate) when the encoded size > Message.MAXLENGTH and truncate=false.",
          "description": "Construct a Message with a very large number of answer records such that the wire format exceeds 65535 bytes. Call the overloaded toWire(maxLength, false) via reflection (or a wrapper) to trigger the branch that checks maxLength and throws MessageSizeExceededException."
        },
        {
          "input": [],
          "expected": "byte array truncated to maxLength bytes with the TC (truncated) flag set in the header.",
          "description": "Same oversized Message as above, but invoke toWire(maxLength, true). The method should truncate the output, set the TC flag, and return a byte array of exactly maxLength bytes, covering the truncation branch."
        }
      ]
    },
    {
      "group": "MessageWithMixedSectionsAndEdgeCases",
      "cases": [
        {
          "input": [],
          "expected": "byte array where sections are written in the order QUESTION, ANSWER, AUTHORITY, ADDITIONAL, and any empty sections are skipped without emitting bytes.",
          "description": "Add records only to the AUTHORITY and ADDITIONAL sections, leaving QUESTION and ANSWER empty. This verifies that the internal loop correctly skips empty sections (branch for `if (sectionCount == 0) continue`)."
        },
        {
          "input": [],
          "expected": "byte array where a Record with a name that requires compression is correctly compressed (pointer offsets are used).",
          "description": "Create two records sharing the same domain name (e.g., two A records for www.example.com). The toWire() method must use DNS name compression for the second occurrence, testing the compression branch inside DNSOutput."
        }
      ]
    }
  ]
}