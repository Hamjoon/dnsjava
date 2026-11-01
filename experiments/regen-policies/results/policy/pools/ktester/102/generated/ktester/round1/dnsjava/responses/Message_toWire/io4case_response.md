{
  "testCases": [
    {
      "group": "Message.toWire(DNSOutput) - header before records order",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "header.toWire(out) is invoked before any Record.toWire; verification done via ordered mock calls",
          "description": "A Message with a non‑null header and a single Record in section 1. The test asserts that the header's toWire method is called first, then the record's toWire, ensuring correct ordering."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - single Compression instance reused",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "The same Compression object (identical by reference) is passed to every Record.toWire call",
          "description": "Message contains three Records spread across sections 0, 2, and 3. Each Record captures the Compression instance it receives; the test verifies that all captured instances are the same object, confirming that toWire creates one Compression and reuses it."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - exception propagation from Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "IOException thrown by a Record.toWire call propagates out of Message.toWire",
          "description": "A Message with a Record whose toWire implementation throws an IOException. The test asserts that Message.toWire does not catch the exception and the same IOException reaches the caller."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - null sections array",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "NullPointerException is thrown when sections field is null",
          "description": "The Message object's sections field is explicitly set to null before calling toWire. Accessing sections.length should cause a NullPointerException."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - null header",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "NullPointerException is thrown because header is null when header.toWire(out) is invoked",
          "description": "A Message with a null header and a valid non‑empty sections array. The method attempts to call header.toWire, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "Message.toWire(DNSOutput) - list contains null Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "out",
              "value": "mock DNSOutput"
            }
          ],
          "expected": "NullPointerException is thrown when iterating over a non‑null list that contains a null Record",
          "description": "Section 0 holds a List with a single null element. The enhanced for‑loop retrieves the null element and attempts to invoke toWire on it, causing a NullPointerException."
        }
      ]
    }
  ]
}