{
    "new_test_cases": [
        {
            "group": "getTSIG - Header count zero but ADDITIONAL list non‑empty",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "The Header reports a count of 0 for the ADDITIONAL section, but the internal list actually contains one or more records. The method should ignore the list and return null because it bases its decision solely on the header count."
                }
            ]
        },
        {
            "group": "getTSIG - Header count smaller than list size, TSIG at counted index",
            "cases": [
                {
                    "input": [],
                    "expected": "TSIGRecord instance",
                    "description": "Header count is 2 while the ADDITIONAL list contains three records. The record at index (count‑1) = 1 is a TSIGRecord, and the third record (index 2) is something else. The method should return the TSIGRecord found at the counted position, demonstrating that it does not look at the true last element of the list."
                }
            ]
        },
        {
            "group": "getTSIG - Last record in list is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The ADDITIONAL list size matches the header count, but the last element stored in the list is null. When the method accesses rec.type it will throw a NullPointerException. This tests robustness against null entries."
                }
            ]
        },
        {
            "group": "getTSIG - Record type TSIG but not a TSIGRecord instance",
            "cases": [
                {
                    "input": [],
                    "expected": "ClassCastException",
                    "description": "The last record in the ADDITIONAL section has its type field set to Type.TSIG, yet the object is a plain Record (or a subclass that is not TSIGRecord). After the type check passes, the method casts to TSIGRecord, causing a ClassCastException. This verifies that the method assumes the type‑to‑class contract."
                }
            ]
        },
        {
            "group": "getTSIG - Header count greater than list size (IndexOutOfBounds)",
            "cases": [
                {
                    "input": [],
                    "expected": "IndexOutOfBoundsException",
                    "description": "Header reports a count of 3 for ADDITIONAL, but the internal list only contains two elements. Accessing l.get(count‑1) triggers an IndexOutOfBoundsException. This is a different mismatch scenario from the existing test that expects an exception due to count > actual size."
                }
            ]
        }
    ]
}