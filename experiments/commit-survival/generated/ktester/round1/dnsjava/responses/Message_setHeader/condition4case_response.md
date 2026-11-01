Invalid control character at: line 8 column 43 (char 182)
{
    "group": "Message.setHeader",
    "cases": [
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "new Header() // default header"
                }
            ],
            "expected": "Message.getHeader() returns the same Header instance that was passed",
            "description": "Verify that setHeader stores a non‑null Header object and that getHeader returns the exact same reference."
        },
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "null"
                }
            ],
            "expected": "Message.getHeader() returns null",
            "description": "Ensure that passing null to setHeader clears the header field without throwing an exception."
        },
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "header1 = new Header(); header2 = new Header();"
                }
            ],
            "expected": "After first call, getHeader() returns header1; after second call, getHeader() returns header2",
            "description": "Check that a second call to setHeader replaces the previous Header instance."
        },
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "header = new Header(); header.setID(12345);"
                }
            ],
            "expected": "Message.getHeader().getID() == 12345 (reflects modifications made to the original Header object)",
            "description": "Confirm that setHeader stores the reference, not a copy, so later changes to the Header object are visible through the Message."
        }
    ]
}