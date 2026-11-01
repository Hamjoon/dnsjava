{
    "group": "Valid sections with existing records",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 0
                }
            ],
            "expected": "unmodifiable list containing the added Record objects (size = 2); attempts to modify the list should throw UnsupportedOperationException",
            "description": "Add two dummy Record instances to the ANSWER section (index 0) using addRecord, then call getSection(0). The method should return an unmodifiable view of the internal list, preserving the two records."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 1
                }
            ],
            "expected": "unmodifiable list containing the added Record object (size = 1); attempts to modify the list should throw UnsupportedOperationException",
            "description": "Add a single dummy Record to the AUTHORITY section (index 1). Verify that getSection(1) returns an unmodifiable list with exactly that record."
        }
    ]
}