{
    "cases": [
        {
            "group": "Message.getRcode - null header handling",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The Message instance has a null Header (header field not set). Calling getRcode() should throw a NullPointerException when it attempts to invoke header.getRcode()."
                }
            ]
        },
        {
            "group": "Message.getRcode - mixed values without OPT",
            "cases": [
                {
                    "input": [],
                    "expected": "5",
                    "description": "Header returns rcode 5 and no OPT record is present (getOPT() returns null). The method should return the header rcode unchanged."
                },
                {
                    "input": [],
                    "expected": "7",
                    "description": "Header returns rcode 7 and no OPT record is present. Verifies that arbitrary non‑zero header rcodes are propagated correctly."
                }
            ]
        },
        {
            "group": "Message.getRcode - mixed values with OPT",
            "cases": [
                {
                    "input": [],
                    "expected": "5",
                    "description": "Header returns rcode 5, OPT record is present but its extendedRcode is 0. Shifted contribution is 0, so the result equals the header rcode."
                },
                {
                    "input": [],
                    "expected": "21",
                    "description": "Header returns rcode 5, OPT extendedRcode is 1 (1 << 4 = 16). Expected result = 5 + 16 = 21."
                },
                {
                    "input": [],
                    "expected": "53",
                    "description": "Header returns rcode 5, OPT extendedRcode is 3 (3 << 4 = 48). Expected result = 5 + 48 = 53."
                },
                {
                    "input": [],
                    "expected": "300",
                    "description": "Header returns rcode 20, OPT extendedRcode is 5 (5 << 4 = 80). Expected result = 20 + 80 = 100."
                },
                {
                    "input": [],
                    "expected": "1020",
                    "description": "Header returns rcode 100, OPT extendedRcode is 23 (23 << 4 = 368). Expected result = 100 + 368 = 468."
                }
            ]
        },
        {
            "group": "Message.getRcode - extreme values and overflow handling",
            "cases": [
                {
                    "input": [],
                    "expected": "2147483647",
                    "description": "Header returns Integer.MAX_VALUE, OPT is null. The method should simply return the header value without overflow."
                },
                {
                    "input": [],
                    "expected": "-2147483549",
                    "description": "Header returns Integer.MAX_VALUE, OPT extendedRcode is 15 (15 << 4 = 240). Adding 240 causes integer overflow, resulting in -2147483549. This verifies that getRcode() does not guard against overflow."
                },
                {
                    "input": [],
                    "expected": "-2147483648",
                    "description": "Header returns Integer.MIN_VALUE, OPT is null. The method should return Integer.MIN_VALUE unchanged."
                },
                {
                    "input": [],
                    "expected": "-2147483528",
                    "description": "Header returns Integer.MIN_VALUE, OPT extendedRcode is 15 (240). Adding 240 to Integer.MIN_VALUE yields -2147483528, confirming arithmetic underflow handling."
                },
                {
                    "input": [],
                    "expected": "-16",
                    "description": "Header returns -16, OPT extendedRcode is 0. Ensures that negative header rcodes (though atypical) are propagated unchanged."
                },
                {
                    "input": [],
                    "expected": "-8",
                    "description": "Header returns -16, OPT extendedRcode is 1 (16). Expected result = -16 + 16 = 0, but because -16 + 16 = 0, the expected value is 0. This case checks correct addition when the shift cancels a negative header."
                }
            ]
        }
    ]
}