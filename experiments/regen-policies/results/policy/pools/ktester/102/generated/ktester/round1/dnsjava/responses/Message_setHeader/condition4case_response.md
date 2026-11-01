{
    "group": "Message.setHeader tests",
    "cases": [
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "new Header()" 
                }
            ],
            "expected": "Message.getHeader() returns the same Header instance that was passed to setHeader",
            "description": "Verify that a non‑null Header object is correctly stored in the Message instance."
        },
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "null"
                }
            ],
            "expected": "Message.getHeader() returns null",
            "description": "Verify that passing null to setHeader clears the header field without throwing an exception."
        }
    ]
}