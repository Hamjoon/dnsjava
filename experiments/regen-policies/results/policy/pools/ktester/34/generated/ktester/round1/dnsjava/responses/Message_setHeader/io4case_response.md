{
    "group": "testSetHeader",
    "cases": [
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "new Header()"
                }
            ],
            "expected": "Message.getHeader() returns the same Header instance that was passed",
            "description": "Verify that a freshly created Header object is stored correctly and can be retrieved via getHeader()."
        },
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "null"
                }
            ],
            "expected": "Message.getHeader() returns null",
            "description": "Ensure that passing null to setHeader clears the header field and getHeader() reflects the null value."
        },
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "header1 = new Header(); header2 = new Header()"
                }
            ],
            "expected": "Message.getHeader() returns header2 (the most recently set Header)",
            "description": "Test that calling setHeader twice replaces the previous Header with the new one."
        },
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "header = new Header(); header.setRcode(2)"
                }
            ],
            "expected": "Message.getHeader().getRcode() == 2 after modification of the original Header instance",
            "description": "Confirm that setHeader stores a reference to the supplied Header object, so subsequent changes to the original Header are visible through the Message."
        }
    ]
}