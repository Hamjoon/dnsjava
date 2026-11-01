{
    "removeRecord_nullSectionsArray": [
        {
            "group": "removeRecord_nullSectionsArray",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "mockRecord"
                        },
                        {
                            "parameter": "section",
                            "value": "0"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The internal `sections` array is null. After `Section.check` passes, the method attempts to access `sections[section]` which throws a NullPointerException."
                }
            ]
        }
    ],
    "removeRecord_nullHeader": [
        {
            "group": "removeRecord_nullHeader",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "mockRecord"
                        },
                        {
                            "parameter": "section",
                            "value": "1"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The `header` field is null while the target section list contains the record. Removal succeeds, but the subsequent call to `header.decCount(section)` triggers a NullPointerException."
                }
            ]
        }
    ],
    "removeRecord_headerDecCountThrows": [
        {
            "group": "removeRecord_headerDecCountThrows",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "mockRecord"
                        },
                        {
                            "parameter": "section",
                            "value": "2"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The header's count for the given section is already zero. After the record is removed, `header.decCount(section)` attempts to decrement below zero and throws IllegalArgumentException."
                }
            ]
        }
    ],
    "removeRecord_sectionsArrayTooShort": [
        {
            "group": "removeRecord_sectionsArrayTooShort",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "mockRecord"
                        },
                        {
                            "parameter": "section",
                            "value": "3"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "The `sections` array is deliberately created with a length smaller than the valid section index (e.g., length 2). `Section.check` passes, but accessing `sections[section]` results in an ArrayIndexOutOfBoundsException."
                }
            ]
        }
    ]
}