{
  "testCases": [
    {
      "group": "Message.toWire(DNSOutput) - null record handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Header.toWire(out) is invoked once; iteration encounters a null Record in a non‑empty section and throws NullPointerException, propagating the exception.",
          "description": "A section list contains a null element; the method should not silently ignore it and must propagate the NPE caused by invoking rec.toWire on a null reference."
        },
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Header.toWire(out) is invoked once; RecordA.toWire(out, 1, compression) is invoked once; RecordB is null and is skipped without exception.",
          "description": "A section list contains a mix of a valid Record and a null element; the method should invoke toWire on the valid Record and ignore the null entry, not throwing an exception."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - order of header and records",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Header.toWire(out) is invoked before any Record.toWire; verification using InOrder confirms header call precedes RecordX and RecordY calls.",
          "description": "Ensures that the header serialization occurs prior to serializing any records, regardless of how many sections are present."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - separate Compression instances per call",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Two consecutive calls to Message.toWire(out) each create a distinct Compression object; the Compression instance used in the first call is not the same (by reference) as the one used in the second call.",
          "description": "Verifies that the method does not retain a static or cached Compression object across separate invocations, which could corrupt name compression state."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - zero‑length sections array",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Header.toWire(out) is invoked once; no Record.toWire is invoked because sections.length == 0, therefore the outer loop never executes.",
          "description": "Tests the edge case where the Message is constructed with an empty sections array (length 0). The method should handle it gracefully and only write the header."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - multiple sections with varying record counts",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mocked DNSOutput"
            }
          ],
          "expected": "Header.toWire(out) is invoked once; RecordA.toWire(out, 0, compression) once; RecordB.toWire(out, 1, compression) twice; RecordC.toWire(out, 3, compression) once; all calls share the same Compression instance.",
          "description": "Covers all four possible section indices (0‑3) with different numbers of records per section, confirming correct section index is passed and the same Compression object is reused across all calls."
        }
      ]
    }
  ]
}