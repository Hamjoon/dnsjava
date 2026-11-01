{
    "test_cases": [
        {
            "group": "getTSIG - Header is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The Message instance has its internal `header` field set to null. When `getTSIG()` calls `header.getCount(...)`, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "getTSIG - Sections array is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The Message instance's `sections` array reference is null. Accessing `sections[Section.ADDITIONAL]` causes a NullPointerException before any list operations."
                }
            ]
        },
        {
            "group": "getTSIG - Sections array too short (ArrayIndexOutOfBounds)",
            "cases": [
                {
                    "input": [],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "The `sections` array is created with a length smaller than `Section.ADDITIONAL` (e.g., length 1). Accessing `sections[Section.ADDITIONAL]` results in an ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "getTSIG - Additional section list is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The `sections[Section.ADDITIONAL]` entry is explicitly set to null while the header reports a positive count. When `getTSIG()` attempts to assign it to `List<Record> l`, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "getTSIG - Header reports negative count",
            "cases": [
                {
                    "input": [],
                    "expected": "IndexOutOfBoundsException",
                    "description": "A mocked Header returns a negative count (e.g., -2) for the ADDITIONAL section. The method computes `count - 1` and uses it as an index on the list, causing an IndexOutOfBoundsException."
                }
            ]
        }
    ]
}