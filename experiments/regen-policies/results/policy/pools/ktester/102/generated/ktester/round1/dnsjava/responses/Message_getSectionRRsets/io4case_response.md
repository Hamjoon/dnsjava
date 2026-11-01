{
    "tests": [
        {
            "group": "Empty section handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "empty list (Collections.emptyList())",
                    "description": "The requested section has never been populated (sections[1] == null). getSectionRRsets should return an immutable empty list."
                }
            ]
        },
        {
            "group": "Single record in section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "list size 1; the sole RRset contains exactly the added record",
                    "description": "Add a single ARecord to the ANSWER section. The method must create one RRset wrapping that record."
                }
            ]
        },
        {
            "group": "Multiple records belonging to the same RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "list size 1; the RRset contains both added records",
                    "description": "Add two ARecords with identical name, type and class to the ANSWER section. They should be merged into a single RRset containing both records."
                }
            ]
        },
        {
            "group": "Multiple distinct RRsets",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": "list size 3; each RRset contains exactly one of the added records",
                    "description": "Add three records to the AUTHORITY section that differ in name or type (e.g., ARecord, MXRecord, NSRecord). The method must return three separate RRsets."
                }
            ]
        },
        {
            "group": "Mixed same and different RRsets",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": "list size 2; first RRset contains two records of the same set, second RRset contains the distinct record",
                    "description": "Add three records to the ADDITIONAL section: two ARecords with the same name and one TXTRecord with a different name. The method should produce two RRsets – one merged set of the two ARecords and one for the TXTRecord."
                }
            ]
        }
    ]
}