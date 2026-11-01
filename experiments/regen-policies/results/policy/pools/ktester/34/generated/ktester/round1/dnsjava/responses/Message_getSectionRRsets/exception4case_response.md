{
    "testCases": [
        {
            "group": "Null internal sections array causes NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "NullPointerException thrown when accessing sections[section]",
                    "description": "The Message instance is created without initializing the internal `sections` array (e.g., via reflection set to null). Calling getSectionRRsets(0) should raise a NullPointerException because the method attempts to read `sections[section]`."
                }
            ]
        },
        {
            "group": "Insufficient sections array length causes AIOOBE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException thrown when accessing sections[section]",
                    "description": "The Message instance has its `sections` array allocated with a length smaller than the maximum section index (e.g., new List[2]). Calling getSectionRRsets(2) should trigger an ArrayIndexOutOfBoundsException because the method tries to index beyond the array bounds."
                }
            ]
        },
        {
            "group": "Null record in section list causes NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "NullPointerException thrown during iteration when a null Record is encountered",
                    "description": "The `sections[1]` list is non‑null but contains a `null` element. When the method iterates over the list, the first iteration attempts to call `rec.sameRRset(set)` on a null reference, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Record.sameRRset throws RuntimeException propagates",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "RuntimeException (or subclass) thrown by Record.sameRRset propagates out of getSectionRRsets",
                    "description": "Insert a custom subclass of `Record` into `sections[0]` whose `sameRRset(RRset)` method deliberately throws a RuntimeException (e.g., IllegalStateException). The method should not catch this exception; it must propagate to the caller."
                }
            ]
        }
    ]
}