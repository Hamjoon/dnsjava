Invalid control character at: line 8 column 43 (char 174)
{
    "group": "setHeader",
    "cases": [
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "new Header() // a fresh Header instance"
                }
            ],
            "expected": "Message.getHeader() returns the same Header instance that was passed to setHeader",
            "description": "Verify that setting a non‑null Header stores the reference correctly; subsequent calls to getHeader should return the exact same object."
        },
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "null"
                }
            ],
            "expected": "Message.getHeader() returns null",
            "description": "Ensure that passing null to setHeader clears the header field; getHeader should return null after the call."
        },
        {
            "input": [
                {
                    "parameter": "h",
                    "value": "header = new Header(); header.setID(0x1234); // assume Header has setID/getID"
                }
            ],
            "expected": "Message.getHeader().getID() == 0x1234 after setHeader, and if header.setID(0xABCD) is called later, Message.getHeader().getID() reflects the new value",
            "description": "Confirm that Message stores the Header by reference, not a copy. Changing the Header after it has been set should be observable through Message.getHeader()."
        }
    ]
}