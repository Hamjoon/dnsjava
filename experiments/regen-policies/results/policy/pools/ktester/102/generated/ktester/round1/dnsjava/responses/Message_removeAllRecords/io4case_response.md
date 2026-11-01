{
    "test_cases": [
        {
            "group": "removeAllRecords_allValidSections_emptyInitially",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "Section.QUESTION"
                        }
                    ],
                    "expected": "sections[Section.QUESTION] remains null and header count for QUESTION stays 0 (no exception)",
                    "description": "Remove records from the QUESTION section when it is initially empty. Verifies that the method safely handles a null entry without altering state."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": "sections[Section.AUTHORITY] remains null and header count for AUTHORITY stays 0 (no exception)",
                    "description": "Remove records from the AUTHORITY section when it is initially empty. Confirms idempotent behavior for empty sections."
                }
            ]
        },
        {
            "group": "removeAllRecords_invalidSection_negative",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "-1"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Section.check",
                    "description": "Pass a negative section index. The method should reject the value and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "removeAllRecords_invalidSection_outOfRange",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "4"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by Section.check",
                    "description": "Pass a section index greater than the maximum defined (4). The method must validate the range and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "removeAllRecords_afterAddRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": "sections[Section.AUTHORITY] becomes null and header count for AUTHORITY is set to 0 (no exception)",
                    "description": "Add a Record to the AUTHORITY section, then call removeAllRecords. Ensures that the previously populated section is cleared and the header count is reset."
                },
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "Section.QUESTION"
                        }
                    ],
                    "expected": "sections[Section.QUESTION] becomes null and header count for QUESTION is set to 0 (no exception)",
                    "description": "Add a Record to the QUESTION section (as a query), then invoke removeAllRecords. Verifies proper cleanup of the question section."
                }
            ]
        },
        {
            "group": "removeAllRecords_idempotent_multipleCalls",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": "Section.ADDITIONAL"
                        }
                    ],
                    "expected": "After first call: sections[Section.ADDITIONAL] becomes null and header count set to 0; after second call: still null and count remains 0 (no exception)",
                    "description": "Call removeAllRecords twice on the same ADDITIONAL section. Confirms that a second invocation does not cause errors and leaves the state unchanged."
                }
            ]
        }
    ]
}