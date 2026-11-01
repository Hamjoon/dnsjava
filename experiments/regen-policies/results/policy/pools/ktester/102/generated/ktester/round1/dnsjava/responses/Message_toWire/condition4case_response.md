{
  "group": "Message.toWire(DNSOutput) - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "out",
          "value": "DNSOutput mock instance"
        }
      ],
      "expected": "header.toWire(out) is invoked exactly once; no Record.toWire is invoked because all sections are null",
      "description": "All sections in the Message are null. The method should skip the inner loops and only write the header."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "DNSOutput mock instance"
        }
      ],
      "expected": "header.toWire(out) is invoked once; Record.toWire(out, 0, any Compression) is invoked once for the single record in section 0",
      "description": "Section 0 contains a non‑null list with one Record. The method must write the header and then call the record's toWire with section index 0."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "DNSOutput mock instance"
        }
      ],
      "expected": "header.toWire(out) is invoked once; Record.toWire is invoked three times: once for section 0, twice for section 2, with the correct section indexes",
      "description": "Mixed null and non‑null sections: section 0 has one Record, section 1 is null, section 2 has two Records. The method should correctly iterate over each non‑null section and call each record's toWire with its section index."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "DNSOutput mock instance"
        }
      ],
      "expected": "header.toWire(out) is invoked once; no Record.toWire is invoked because the only non‑null section contains an empty list",
      "description": "A non‑null but empty List is present in a section. The method should skip the inner loop without calling any Record.toWire."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "DNSOutput mock instance"
        }
      ],
      "expected": "header.toWire(out) is invoked once; the method completes without entering the for‑loop because sections array length is 0",
      "description": "The Message has an empty sections array (length 0). This tests the edge case where the outer loop never executes."
    }
  ]
}