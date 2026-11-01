{
  "testCases": [
    {
      "group": "Size after setting Header",
      "cases": [
        {
          "input": [],
          "expected": "unchanged",
          "description": "Create a fresh Message, then call setHeader() with a newly constructed Header. The internal size field should remain 0 because setting a Header does not modify the serialized length of the message."
        },
        {
          "input": [],
          "expected": "non‑zero",
          "description": "Create a Message, add a dummy Record to the ANSWER section, then replace the Header with a custom Header (e.g., different ID or flags). The size should stay non‑zero (the Header replacement does not reset the size)."
        }
      ]
    },
    {
      "group": "Size after adding TSIG",
      "cases": [
        {
          "input": [],
          "expected": "non‑zero",
          "description": "Create a Message, invoke setTSIG() with a valid TSIG key. Adding a TSIG record should increase the internal size, therefore numBytes() must return a value greater than 0."
        },
        {
          "input": [],
          "expected": "unchanged",
          "description": "Create a Message with no other sections, call setTSIG() with a null TSIG key. The method should ignore the null argument and leave the size at 0."
        }
      ]
    },
    {
      "group": "Size after removing records",
      "cases": [
        {
          "input": [],
          "expected": 0,
          "description": "Add a dummy Record to the ANSWER section, verify numBytes() > 0, then remove the same Record with removeRecord(). After removal the internal size should be back to 0."
        },
        {
          "input": [],
          "expected": "unchanged",
          "description": "Attempt to remove a Record that was never added. The size must remain unchanged (still 0)."
        }
      ]
    },
    {
      "group": "Size after cloning",
      "cases": [
        {
          "input": [],
          "expected": "same as original",
          "description": "Create a Message, add at least one Record, capture its size, then clone the Message. The cloned instance must report the identical size via numBytes()."
        },
        {
          "input": [],
          "expected": 0,
          "description": "Clone a freshly constructed empty Message. Both the original and the clone should report size 0."
        }
      ]
    },
    {
      "group": "Size after toWire serialization",
      "cases": [
        {
          "input": [],
          "expected": "equals byte array length",
          "description": "Add one or more Records, call toWire() to obtain the serialized byte array, and compare numBytes() with the length of that array. They must match because toWire() should not alter the stored size."
        }
      ]
    },
    {
      "group": "Size after toWire with maxLength truncation",
      "cases": [
        {
          "input": [],
          "expected": "≤ maxLength",
          "description": "Construct a Message whose logical size exceeds 1024 bytes, then invoke toWire(1024). The returned byte array length will be at most 1024, and numBytes() must reflect the truncated length (i.e., be ≤ 1024)."
        }
      ]
    },
    {
      "group": "Size after normalize()",
      "cases": [
        {
          "input": [],
          "expected": "non‑increasing",
          "description": "Create a Message with extra, irrelevant records, call normalize(query) using a valid query Message. Normalization removes irrelevant data, so the size reported by numBytes() after normalization must be less than or equal to the size before the call."
        }
      ]
    },
    {
      "group": "Size after setting Resolver",
      "cases": [
        {
          "input": [],
          "expected": "unchanged",
          "description": "Assign a mock Resolver to a Message via setResolver(). The resolver is not part of the wire format, therefore the internal size must stay unchanged."
        }
      ]
    }
  ]
}