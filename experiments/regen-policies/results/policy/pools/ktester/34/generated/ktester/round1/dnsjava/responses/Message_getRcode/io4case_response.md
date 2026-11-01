{
  "test_cases": [
    {
      "group": "Message.getRcode - header max rcode (15) no OPT",
      "cases": [
        {
          "input": [],
          "expected": "15",
          "description": "Message whose Header returns the maximum standard DNS rcode (15) and has no OPT record. The method should return the header rcode unchanged."
        }
      ]
    },
    {
      "group": "Message.getRcode - header max rcode (15) with OPT extended rcode 0",
      "cases": [
        {
          "input": [],
          "expected": "15",
          "description": "Message with Header.getRcode() = 15 and an OPTRecord whose getExtendedRcode() returns 0. Since (0 << 4) = 0, the result should remain 15."
        }
      ]
    },
    {
      "group": "Message.getRcode - OPT present with maximum extended rcode (255) and header max (15)",
      "cases": [
        {
          "input": [],
          "expected": "4095",
          "description": "Header returns 15 and OPTRecord returns the maximum 8‑bit extended rcode (255). Expected result = 15 + (255 << 4) = 15 + 4080 = 4095."
        }
      ]
    },
    {
      "group": "Message.getRcode - OPT present with negative extended rcode",
      "cases": [
        {
          "input": [],
          "expected": "-14",
          "description": "Header returns 2 and OPTRecord returns a negative extended rcode (-1). The shift yields (-1 << 4) = -16, so the final rcode should be 2 + (-16) = -14. This tests handling of unexpected negative values."
        }
      ]
    },
    {
      "group": "Message.getRcode - null Header should throw NullPointerException",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "Message is constructed with a null Header. Calling getRcode() should raise a NullPointerException because the method dereferences header without a null check."
        }
      ]
    }
  ]
}