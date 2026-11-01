{
  "group": "Message.getRcode - branch and arithmetic coverage",
  "cases": [
    {
      "input": [],
      "expected": "0",
      "description": "Header returns rcode 0 and no OPT record is present (opt == null). The method should return the header rcode unchanged."
    },
    {
      "input": [],
      "expected": "2",
      "description": "Header returns rcode 2 and no OPT record is present. The method should return 2."
    },
    {
      "input": [],
      "expected": "16",
      "description": "Header returns rcode 0, an OPT record is present with extendedRcode = 1. Since extendedRcode is shifted left by 4 bits (1 << 4 = 16), the method should return 0 + 16 = 16."
    },
    {
      "input": [],
      "expected": "35",
      "description": "Header returns rcode 3, an OPT record is present with extendedRcode = 2. The shifted value is 2 << 4 = 32, so the method should return 3 + 32 = 35."
    },
    {
      "input": [],
      "expected": "255",
      "description": "Header returns the maximum 4‑bit rcode 15, an OPT record is present with the maximum extendedRcode 15. Shifted value = 15 << 4 = 240, result = 15 + 240 = 255."
    },
    {
      "input": [],
      "expected": "0",
      "description": "Header returns rcode 0, an OPT record is present but its extendedRcode is 0. The shifted contribution is 0, so the method returns 0."
    }
  ]
}