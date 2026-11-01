{
  "group": "Message.toWire(DNSOutput) - header and section handling",
  "cases": [
    {
      "input": [
        {
          "parameter": "out",
          "value": "mocked DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) is invoked exactly once; no Record.toWire is invoked because all sections are null.",
      "description": "All sections are null – the method should only write the header and skip the outer loop without invoking any Record."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "mocked DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) is invoked once; RecordA.toWire(out, 1, compression) and RecordB.toWire(out, 1, compression) are each invoked exactly once; sections[0] is null and therefore skipped.",
      "description": "Section 0 is null, section 1 contains two records. The method must skip the null section and correctly write both records using the same Compression instance."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "mocked DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) is invoked once; no Record.toWire is invoked because the only non‑null section contains an empty list.",
      "description": "A non‑null but empty list should cause the inner loop to execute zero times, resulting in no record serialization."
    },
    {
      "input": [
        {
          "parameter": "out",
          "value": "mocked DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) is invoked once; RecordX.toWire(out, 0, compression) and RecordY.toWire(out, 2, compression) are each invoked once; the Compression instance passed to both calls is the same object (reference equality).",
      "description": "Multiple non‑null sections (0 and 2) each contain one record. The test verifies that the same Compression object is reused across all Record.toWire calls, satisfying the contract of the method."
    }
  ]
}