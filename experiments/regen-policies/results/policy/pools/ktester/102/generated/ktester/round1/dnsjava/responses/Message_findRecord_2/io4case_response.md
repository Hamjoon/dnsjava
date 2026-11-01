{
    "newTestCases": [
        {
            "group": "findRecord - record found in multiple sections (early exit)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=10, name=\"multi.com.\", type=A}"
                        }
                    ],
                    "expected": true,
                    "description": "The target record is added to both the ANSWER and AUTHORITY sections. The method should return true after checking the ANSWER section and must not continue to later sections."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=11, name=\"multi.org.\", type=MX}"
                        }
                    ],
                    "expected": true,
                    "description": "The target record appears in ANSWER, AUTHORITY and ADDITIONAL sections simultaneously. The loop should stop at the first match (ANSWER) and return true."
                }
            ]
        },
        {
            "group": "findRecord - null record argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": null
                        }
                    ],
                    "expected": false,
                    "description": "A null Record is passed. All non‑null sections contain regular records, but `contains(null)` never matches, so the method should safely return false without throwing an exception."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": null
                        }
                    ],
                    "expected": false,
                    "description": "All three sections are null and the argument is null. The method should simply iterate, skip null sections and finally return false."
                }
            ]
        },
        {
            "group": "findRecord - sections contain null entries",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=12, name=\"nullentry.com.\", type=TXT}"
                        }
                    ],
                    "expected": true,
                    "description": "The ANSWER list contains a `null` element followed by the target record. `List.contains` correctly identifies the record despite the preceding null, and the method returns true."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=13, name=\"onlynull.com.\", type=AAAA}"
                        }
                    ],
                    "expected": false,
                    "description": "The ANSWER list contains only a `null` element; the target record is not present in any section. The method should return false."
                }
            ]
        },
        {
            "group": "findRecord - mixed null and empty sections with record in last section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=14, name=\"last.com.\", type=SRV}"
                        }
                    ],
                    "expected": true,
                    "description": "ANSWER section is null, AUTHORITY section is an empty list, ADDITIONAL section contains the target record. The method must continue past null and empty sections and finally return true."
                }
            ]
        }
    ]
}