{
    "group": "Null header handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance with header set to null (no OPT record, not signed)"
                }
            ],
            "expected": "NullPointerException",
            "description": "Invoking toString() when the Message's header field is null should raise a NullPointerException because the method unconditionally calls header.toStringWithRcode(...) or header.toString()."
        }
    ]
}