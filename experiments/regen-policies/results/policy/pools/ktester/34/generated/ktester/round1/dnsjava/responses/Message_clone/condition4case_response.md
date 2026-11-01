{
    "group": "Clone with empty sections and no TSIG records",
    "cases": [
        {
            "input": [],
            "expected": "A new Message instance whose sections array has the same length as the original, all entries are null; header is a distinct clone (different object but equal content); querytsig and generatedTsig are null. Modifying the original's header or sections after cloning does not affect the clone.",
            "description": "Verify that clone() correctly handles the case where `sections` contains only null entries and both TSIG fields are null. The method should create a new sections array with nulls, clone the header, and leave TSIG fields null."
        }
    ]
}