{
  "group": "Message.toWire(DNSOutput) - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "message",
          "value": "Message instance whose header is a mock Header (verifies toWire called) and sections array of length 3 where all entries are null"
        },
        {
          "parameter": "out",
          "value": "Mock DNSOutput (verifies no write operations beyond header)"
        }
      ],
      "expected": "Header.toWire(out) is invoked exactly once; no Record.toWire calls are made because every section is null; method completes without exception",
      "description": "All sections are null, exercising the `if (sections[i] == null) continue;` branch for every iteration, confirming that only the header is serialized."
    },
    {
      "input": [
        {
          "parameter": "message",
          "value": "Message instance with a mock Header and sections array of length 2:\n  - sections[0] contains two mock Record objects R0a, R0b\n  - sections[1] contains one mock Record object R1a"
        },
        {
          "parameter": "out",
          "value": "Mock DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) called once; Compression instance C created once and passed to each Record.toWire call; calls occur in order: R0a.toWire(out,0,C), R0b.toWire(out,0,C), R1a.toWire(out,1,C); total of three Record.toWire invocations; method completes without exception",
      "description": "Non‑null sections contain records, testing the normal path where records are serialized and verifying that the same Compression object is reused across calls."
    },
    {
      "input": [
        {
          "parameter": "message",
          "value": "Message instance with a mock Header and sections array of length 3:\n  - sections[0] = null\n  - sections[1] contains one mock Record R1\n  - sections[2] = null"
        },
        {
          "parameter": "out",
          "value": "Mock DNSOutput"
        }
      ],
      "expected": "Header.toWire(out) called once; only R1.toWire(out,1,C) invoked; sections[0] and sections[2] are skipped via the null‑check branch; method completes without exception",
      "description": "Mixed null and non‑null sections, confirming that the `continue` correctly bypasses null entries while processing existing records."
    },
    {
      "input": [
        {
          "parameter": "message",
          "value": "Message instance with a mock Header and a non‑null sections array (any content)"
        },
        {
          "parameter": "out",
          "value": "null"
        }
      ],
      "expected": "NullPointerException thrown (header.toWire receives null DNSOutput)",
      "description": "Passing a null DNSOutput verifies that the method does not guard against null and propagates the NPE from Header.toWire."
    }
  ]
}