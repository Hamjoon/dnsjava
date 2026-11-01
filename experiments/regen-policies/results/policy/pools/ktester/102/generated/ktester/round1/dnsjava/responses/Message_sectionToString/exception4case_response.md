{
    "group": "sectionToString_invalidSection_exceptions",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "-2147483648"
                }
            ],
            "expected": "IllegalArgumentException indicating an invalid section index (Section.check should reject Integer.MIN_VALUE)",
            "description": "Verify that passing Integer.MIN_VALUE as the section causes Section.check to throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "2147483647"
                }
            ],
            "expected": "IllegalArgumentException indicating an invalid section index (Section.check should reject Integer.MAX_VALUE)",
            "description": "Verify that passing Integer.MAX_VALUE as the section causes Section.check to throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "4"
                }
            ],
            "expected": "IllegalArgumentException indicating an invalid section index (Section.check should reject the value just beyond the highest valid section)",
            "description": "Confirm that a section value equal to the number of defined sections (e.g., 4) is considered invalid and triggers an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "100"
                }
            ],
            "expected": "IllegalArgumentException indicating an invalid section index (Section.check should reject arbitrary out‑of‑range values)",
            "description": "Ensure that a large out‑of‑range section value (e.g., 100) is rejected by Section.check with an IllegalArgumentException."
        }
    ]
}