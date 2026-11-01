{
    "testCases": [
        {
            "group": "getTSIG - No additional records",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Message has an empty ADDITIONAL section (header count for ADDITIONAL is 0). The method should return null."
                }
            ]
        },
        {
            "group": "getTSIG - Last additional record is not a TSIG",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Message contains at least one record in the ADDITIONAL section, but the last record's type is not Type.TSIG. The method should return null."
                }
            ]
        },
        {
            "group": "getTSIG - Valid TSIG record present",
            "cases": [
                {
                    "input": [],
                    "expected": "TSIGRecord instance",
                    "description": "Message's ADDITIONAL section contains one or more records and the last record is a TSIGRecord (type == Type.TSIG). The method should return that TSIGRecord."
                }
            ]
        },
        {
            "group": "getTSIG - Multiple additional records, TSIG not last",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Message's ADDITIONAL section has several records where a TSIGRecord exists but is not the last element. Since the method only checks the last record, it should return null."
                }
            ]
        },
        {
            "group": "getTSIG - Header count mismatch with actual list size",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Header reports a count greater than the actual size of the ADDITIONAL list (simulated by mocking Header). The method will attempt to access an index that does not exist, causing an IndexOutOfBoundsException. This test verifies that such a mismatch leads to an exception rather than returning a value."
                }
            ]
        }
    ]
}