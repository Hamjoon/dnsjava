{
    "group": "Message.getRcode - additional and edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "headerRcode",
                    "value": 7
                },
                {
                    "parameter": "optExtendedRcode",
                    "value": 0
                }
            ],
            "expected": 7,
            "description": "OPT record is present but its extended rcode is 0; the method should return the header rcode unchanged (7)."
        },
        {
            "input": [
                {
                    "parameter": "headerRcode",
                    "value": 255
                },
                {
                    "parameter": "optExtendedRcode",
                    "value": 255
                }
            ],
            "expected": 4335,
            "description": "Maximum 8‑bit values for both header rcode and extended rcode. 255 + (255 << 4) = 255 + 4080 = 4335."
        },
        {
            "input": [
                {
                    "parameter": "headerRcode",
                    "value": -3
                },
                {
                    "parameter": "optExtendedRcode",
                    "value": 2
                }
            ],
            "expected": 29,
            "description": "Header returns a negative rcode while an OPT record adds a positive extended rcode. Result = -3 + (2 << 4) = -3 + 32 = 29."
        },
        {
            "input": [
                {
                    "parameter": "headerRcode",
                    "value": 2147483647
                },
                {
                    "parameter": "optExtendedRcode",
                    "value": 1
                }
            ],
            "expected": -2147483633,
            "description": "Integer overflow case: Integer.MAX_VALUE (2,147,483,647) plus (1 << 4) wraps around to -2,147,483,633 according to two's‑complement arithmetic."
        },
        {
            "input": [
                {
                    "parameter": "headerRcode",
                    "value": 10
                },
                {
                    "parameter": "optExtendedRcode",
                    "value": -1
                }
            ],
            "expected": -6,
            "description": "Extended rcode is negative (simulating a malformed OPT record). Calculation: 10 + ((-1) << 4) = 10 + (-16) = -6."
        }
    ]
}