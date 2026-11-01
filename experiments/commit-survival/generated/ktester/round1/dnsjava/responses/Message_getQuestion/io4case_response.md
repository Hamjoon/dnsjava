{
    "groups": [
        {
            "group": "Message.getQuestion_nullSectionsArray",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the internal `sections` array itself is null, calling `getQuestion()` should throw a NullPointerException because the code attempts to index into a null array."
                }
            ]
        },
        {
            "group": "Message.getQuestion_insufficientSectionsArray",
            "cases": [
                {
                    "input": [],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "When the `sections` array is created with length 0 (or any length that does not contain an entry for `Section.QUESTION`), accessing `sections[Section.QUESTION]` should raise an ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "Message.getQuestion_firstElementNull",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "When the QUESTION section list exists but its first element is `null`, `getQuestion()` should return `null` (the list is not empty, but the stored record is null)."
                }
            ]
        },
        {
            "group": "Message.getQuestion_multipleRecords",
            "cases": [
                {
                    "input": [],
                    "expected": "firstRecord",
                    "description": "When the QUESTION section contains several `Record` objects, `getQuestion()` must return the first record in the list, not any subsequent ones."
                }
            ]
        },
        {
            "group": "Message.getQuestion_afterRemoval",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "After adding a `Record` to the QUESTION section and then removing all records from that section, `getQuestion()` should again return `null`."
                }
            ]
        },
        {
            "group": "Message.getQuestion_referenceEquality",
            "cases": [
                {
                    "input": [],
                    "expected": "sameInstance",
                    "description": "When a specific `Record` instance is added to the QUESTION section, `getQuestion()` should return the exact same object reference (==), confirming no copy is made."
                }
            ]
        }
    ]
}