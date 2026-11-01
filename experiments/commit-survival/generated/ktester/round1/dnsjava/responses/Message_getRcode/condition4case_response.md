{
    "group": "Message.getRcode",
    "cases": [
        {
            "input": [],
            "expected": 0,
            "description": "Header returns rcode 0 and no OPT record is present; method should return 0."
        },
        {
            "input": [],
            "expected": 3,
            "description": "Header returns rcode 3 and no OPT record is present; method should return 3."
        },
        {
            "input": [],
            "expected": 16,
            "description": "Header returns rcode 0, OPT record is present with extendedRcode 1; 1 << 4 = 16, so result should be 16."
        },
        {
            "input": [],
            "expected": 82,
            "description": "Header returns rcode 2, OPT record is present with extendedRcode 5; 5 << 4 = 80, 80 + 2 = 82."
        },
        {
            "input": [],
            "expected": 255,
            "description": "Header returns rcode 15, OPT record is present with extendedRcode 15; 15 << 4 = 240, 240 + 15 = 255."
        },
        {
            "input": [],
            "expected": 340,
            "description": "Header returns rcode 20, OPT record is present with extendedRcode 20; 20 << 4 = 320, 320 + 20 = 340."
        },
        {
            "input": [],
            "expected": -5,
            "description": "Header returns a negative rcode (-5) and no OPT record; method should simply return -5 (branch where opt is null)."
        }
    ]
}