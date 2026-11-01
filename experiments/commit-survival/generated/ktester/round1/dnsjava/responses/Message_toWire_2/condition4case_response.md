{
  "testCases": [
    {
      "group": "Empty Message",
      "cases": [
        {
          "input": [],
          "expected": "byte array of length 12 (DNS header only) and Message.size equals 12",
          "description": "A newly instantiated Message with no sections or TSIG. toWire() should serialize only the DNS header (12 bytes) and set the internal size field accordingly."
        }
      ]
    },
    {
      "group": "Message with Question Section Only",
      "cases": [
        {
          "input": [],
          "expected": "byte array length >12 containing the question record; Message.size equals the byte array length",
          "description": "Create a Message via Message.newQuery(Record) with a simple A query. The serialized wire format must contain the header plus one question entry. Verify that size matches the returned array length."
        }
      ]
    },
    {
      "group": "Message with Answer Section",
      "cases": [
        {
          "input": [],
          "expected": "byte array length >12 containing header, question and one answer RR; Message.size equals array length",
          "description": "Add a single answer Record to the ANSWER section of a query Message. toWire() must include the answer section and correctly update size."
        }
      ]
    },
    {
      "group": "Message with Multiple Sections (Answer, Authority, Additional)",
      "cases": [
        {
          "input": [],
          "expected": "byte array containing header, question, answer, authority and additional sections; Message.size matches length",
          "description": "Populate ANSWER, AUTHORITY and ADDITIONAL sections with at least one Record each. Verify that toWire() serializes all sections in the correct order and updates size."
        }
      ]
    },
    {
      "group": "Message with unsigned TSIG (TSIG present but not signed)",
      "cases": [
        {
          "input": [],
          "expected": "byte array containing TSIG record in the additional section; Message.size matches length; no exception thrown",
          "description": "Create a Message, add a TSIG key via setTSIG(TSIG) without signing. toWire() should include the TSIG record as an unsigned additional record."
        }
      ]
    },
    {
      "group": "Message with signed TSIG",
      "cases": [
        {
          "input": [],
          "expected": "byte array containing a signed TSIG record; Message.size matches length; TSIG state is TSIG_SIGNED",
          "description": "Create a Message, sign it with a TSIG key using setTSIG(key, error, querytsig). toWire() must produce a wire format where the TSIG is the last additional record and the internal tsigState reflects TSIG_SIGNED."
        }
      ]
    },
    {
      "group": "Message Exceeding Maximum Length",
      "cases": [
        {
          "input": [],
          "expected": "throws IOException (or IllegalArgumentException) because the generated wire format exceeds Message.MAXLENGTH (65535)",
          "description": "Construct a Message with a very large number of records (e.g., 2000 TXT records) so that the resulting wire length is >65535. Calling toWire() should detect the overflow and raise an exception."
        }
      ]
    },
    {
      "group": "Message with OPT Record (EDNS0)",
      "cases": [
        {
          "input": [],
          "expected": "byte array containing an OPT record in the additional section; Message.size matches length",
          "description": "Add an OPTRecord via setOPT or by directly adding to the additional section. Verify that toWire() includes the OPT pseudo‑RR and updates size correctly."
        }
      ]
    },
    {
      "group": "Message with Truncated Flag Set",
      "cases": [
        {
          "input": [],
          "expected": "byte array where the header's TC flag is set; Message.size matches length",
          "description": "Set the Header's truncated flag (TC) to true before serialization. toWire() must preserve this flag in the output bytes."
        }
      ]
    }
  ]
}