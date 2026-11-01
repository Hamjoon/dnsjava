{
  "testCases": [
    {
      "group": "Message.getRcode - no OPT record (opt == null)",
      "cases": [
        {
          "input": [],
          "expected": "0",
          "description": "Message with a Header whose getRcode() returns 0 and no OPT record present. The method should return the header rcode unchanged (0)."
        },
        {
          "input": [],
          "expected": "2",
          "description": "Message with a Header whose getRcode() returns 2 and no OPT record present. The method should return the header rcode unchanged (2)."
        }
      ]
    },
    {
      "group": "Message.getRcode - OPT record present with extended rcode = 0",
      "cases": [
        {
          "input": [],
          "expected": "5",
          "description": "Message with Header.getRcode() = 5 and an OPTRecord whose getExtendedRcode() returns 0. Since (0 << 4) = 0, the result should be 5."
        }
      ]
    },
    {
      "group": "Message.getRcode - OPT record present with non‑zero extended rcode",
      "cases": [
        {
          "input": [],
          "expected": "16",
          "description": "Header.getRcode() = 0, OPTRecord.getExtendedRcode() = 1. Expected result = 0 + (1 << 4) = 16."
        },
        {
          "input": [],
          "expected": "35",
          "description": "Header.getRcode() = 3, OPTRecord.getExtendedRcode() = 2. Expected result = 3 + (2 << 4) = 3 + 32 = 35."
        },
        {
          "input": [],
          "expected": "75",
          "description": "Header.getRcode() = 15, OPTRecord.getExtendedRcode() = 3. Expected result = 15 + (3 << 4) = 15 + 48 = 63."
        },
        {
          "input": [],
          "expected": "975",
          "description": "Header.getRcode() = 15, OPTRecord.getExtendedRcode() = 60 (max 8‑bit value). Expected result = 15 + (60 << 4) = 15 + 960 = 975."
        }
      ]
    }
  ],
  "setupNotes": "Each test case creates a Message instance, injects a custom Header implementation returning the desired base rcode, and injects a custom OPTRecord (or leaves it null) returning the desired extended rcode. Injection can be performed via reflection to set the private 'header' field and to add the OPTRecord to the message's additional section (or by overriding getOPT() in a test subclass). No method parameters are required for getRcode()."
}