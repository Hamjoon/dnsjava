{
  "testCases": [
    {
      "group": "Message with multiple Question records",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array whose length equals 12 (header) plus the combined wire length of all Question records. The Message.size field must match that length. The wire format must contain the questions in the order they were added.",
          "description": "Add two distinct Question records (e.g., \"example.com. A\" and \"example.org. AAAA\") to the QUESTION section and verify that toWire() serialises both questions after the header."
        }
      ]
    },
    {
      "group": "Message with Authority and Additional sections",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array whose length equals 12 (header) + question length + answer length + authority length + additional length. The sections must appear in the DNS prescribed order: QUESTION, ANSWER, AUTHORITY, ADDITIONAL. Message.size must equal the total length.",
          "description": "Create a Message with one Question, one Answer (A record), one Authority (NS record) and one Additional (A record for the NS). Verify that toWire() respects section ordering and size calculation."
        }
      ]
    },
    {
      "group": "Message with both OPT and TSIG records",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array whose last RR is a TSIG record and whose second‑last RR is the OPT pseudo‑record. The total length equals 12 (header) + question + answer + OPT length + TSIG length. Message.size must match the array length.",
          "description": "Add a Question and an Answer, then set an OPT record via setOPT() and finally sign the message with setTSIG(key). Verify that toWire() places OPT before TSIG and that size reflects the full message."
        }
      ]
    },
    {
      "group": "Message with name compression",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array whose length is smaller than the sum of the individual wire lengths of the Question and Answer because the domain name is reused via compression pointers. Message.size must equal the actual array length.",
          "description": "Add a Question for \"example.com.\" and an Answer (A record) for the same name. The DNSOutput implementation should compress the repeated name. Verify that the resulting byte array is shorter than a naïve concatenation and that size matches."
        }
      ]
    },
    {
      "group": "Message exactly at MAXLENGTH",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array of length 65535 (Message.MAXLENGTH) and Message.size set to 65535. No exception is thrown.",
          "description": "Construct a Message whose header plus records sum to exactly 65535 bytes (e.g., a Question plus many large TXT records). Call toWire() and confirm that the method succeeds at the boundary."
        }
      ]
    },
    {
      "group": "toWire with maxLength limit smaller than message size",
      "cases": [
        {
          "input": [
            {
              "parameter": "maxLength",
              "value": "5000"
            }
          ],
          "expected": "IOException (or a RuntimeException wrapped by SneakyThrows) indicating that the message exceeds the supplied maxLength.",
          "description": "Create a Message whose wire length is greater than 5000 bytes (e.g., by adding many TXT records). Invoke the overloaded toWire(int maxLength) with 5000 and verify that an exception is thrown."
        }
      ]
    },
    {
      "group": "Signed TSIG with error code",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null byte array whose final RR is a TSIG record containing the supplied error code (e.g., BADTIME). The total length equals header + other sections + TSIG length, and Message.size matches this length.",
          "description": "Create a Message, add a Question and an Answer, then call setTSIG(key, Rcode.BADTIME, queryTsig) to sign the message with a non‑zero error. Verify that toWire() includes the TSIG with the error code and that size is correct."
        }
      ]
    }
  ]
}