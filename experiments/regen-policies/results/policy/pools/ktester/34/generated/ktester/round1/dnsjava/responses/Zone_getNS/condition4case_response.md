{
    "group": "Zone.getNS() – basic functionality and copy semantics",
    "cases": [
        {
            "input": [],
            "expected": "throws NullPointerException",
            "description": "When the internal NS field is null, getNS() should propagate the NPE that occurs when the RRset constructor receives a null argument. This verifies that the method does not silently swallow the error."
        },
        {
            "input": [],
            "expected": "RRset instance that is equal (same records) to the internal NS but not the same object reference",
            "description": "Populate the private NS field with a non‑empty RRset (using reflection or addRRset). getNS() must return a new RRset that contains the same records, confirming that a defensive copy is created."
        },
        {
            "input": [],
            "expected": "original internal NS unchanged after mutating the returned RRset",
            "description": "After calling getNS(), add a new Record to the returned RRset. The internal NS stored inside the Zone should remain unchanged, proving that the copy is deep enough for the test’s purposes."
        },
        {
            "input": [],
            "expected": "RRset instance with zero records when internal NS is an empty RRset",
            "description": "Set the internal NS to an empty RRset (no records). getNS() should return a new empty RRset (size 0), ensuring the method works for the edge case of an empty set."
        }
    ]
}