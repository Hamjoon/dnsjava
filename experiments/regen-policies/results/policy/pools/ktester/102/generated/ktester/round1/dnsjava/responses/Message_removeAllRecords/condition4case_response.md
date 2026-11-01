{
    "group": "removeAllRecords_validSections",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "sections[Section.ANSWER] becomes null and header count for ANSWER is set to 0 (no exception)",
            "description": "Remove records from a section that initially contains a non‑empty list of records. Verifies that the internal array entry is cleared and the header count is reset to zero."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "Section.ADDITIONAL"
                }
            ],
            "expected": "sections[Section.ADDITIONAL] remains null and header count for ADDITIONAL stays 0 (no exception)",
            "description": "Remove records from a section that is already empty (null) and has a zero count. Ensures the method does not alter state incorrectly and does not throw."
        }
    ]
}